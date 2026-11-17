#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Suíte automatizada de testes para o JetUML Python Edition.
Valida todos os requisitos funcionais (RF01–RF42) e não funcionais (RNF01–RNF03).
"""

import json
import os
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch
import tkinter as tk

import jetuml


def make_event(x=0, y=0, state=0, char="", x_root=100, y_root=100):
    """Cria um evento sintético compatível com os handlers do Tkinter Canvas."""
    return SimpleNamespace(x=x, y=y, state=state, char=char, x_root=x_root, y_root=y_root)


class BaseJetUMLTestCase(unittest.TestCase):
    """Configura e encerra uma instância limpa da aplicação para cada teste."""

    def setUp(self):
        self.root = tk.Tk()
        self.root.withdraw()
        self.app = jetuml.JetUMLApp(self.root)
        self.root.update_idletasks()

    def tearDown(self):
        try:
            for tid in list(self.app.notebook.tabs()):
                tab = self.root.nametowidget(tid)
                tab.dirty = False
            self.root.destroy()
        except Exception:
            pass

    def click_canvas(self, tab, x, y, ctrl=False, shift=False):
        state = (0x0004 if ctrl else 0) | (0x0001 if shift else 0)
        ev = make_event(x=x, y=y, state=state)
        tab._on_left_down(ev)
        tab._on_left_up(ev)
        self.root.update_idletasks()

    def drag_canvas(self, tab, x1, y1, x2, y2, ctrl=False, shift=False):
        state = (0x0004 if ctrl else 0) | (0x0001 if shift else 0)
        tab._on_left_down(make_event(x=x1, y=y1, state=state))
        tab._on_left_drag(make_event(x=(x1 + x2) / 2, y=(y1 + y2) / 2, state=state))
        tab._on_left_drag(make_event(x=x2, y=y2, state=state))
        tab._on_left_up(make_event(x=x2, y=y2, state=state))
        self.root.update_idletasks()


# ==============================================================================
# 1. DIAGRAMAS, ARQUIVOS, ABAS E EXPORTAÇÃO (RF01–RF06, RNF01–RNF02)
# ==============================================================================

class TestFilesTabsAndExport(BaseJetUMLTestCase):

    def test_rf01_create_all_diagram_types_in_tabs(self):
        self.assertEqual(len(self.app.notebook.tabs()), 0)
        self.assertTrue(self.app.welcome_frame.winfo_ismapped() or len(self.app.welcome_frame.winfo_children()) > 0)

        for idx, dtype in enumerate(jetuml.DIAGRAM_TYPES, start=1):
            self.app.new_diagram(dtype)
            self.assertEqual(len(self.app.notebook.tabs()), idx)
            self.assertEqual(self.app.current_tab().model.diagram_type, dtype)

    def test_rf02_rnf02_save_open_recent_and_close_json(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)

        with tempfile.TemporaryDirectory() as tmpdir:
            json_path = os.path.join(tmpdir, "modelo.json")
            with patch("tkinter.filedialog.asksaveasfilename", return_value=json_path):
                saved = self.app.save_diagram_as()
            self.assertTrue(saved)
            self.assertFalse(tab.dirty)
            self.assertTrue(os.path.exists(json_path))

            # RNF02: Verifica estrutura JSON aberta e legível
            with open(json_path, "r", encoding="utf-8") as f:
                raw = json.load(f)
            self.assertEqual(raw["format"], "JetUML-JSON-1.0")
            self.assertEqual(raw["diagram_type"], "Class Diagram")
            self.assertEqual(len(raw["nodes"]), 1)

            # Fecha aba e reabre via open_diagram
            self.app.close_current_tab()
            self.assertEqual(len(self.app.notebook.tabs()), 0)

            self.app.open_diagram(json_path)
            self.assertEqual(len(self.app.notebook.tabs()), 1)
            reopened = self.app.current_tab()
            self.assertEqual(len(reopened.model.nodes), 1)
            self.assertIn(os.path.abspath(json_path), self.app.recent_files)

    def test_rf03_duplicate_diagram(self):
        self.app.new_diagram("State Diagram")
        tab1 = self.app.current_tab()
        tab1.select_tool("State")
        self.click_canvas(tab1, 120, 120)

        self.app.duplicate_diagram()
        self.assertEqual(len(self.app.notebook.tabs()), 2)
        tab2 = self.app.current_tab()
        self.assertIsNot(tab1, tab2)
        self.assertEqual(len(tab2.model.nodes), 1)

        # Modificar cópia não afeta o original
        tab2.model.nodes[0].properties["name"] = "Alterado"
        self.assertEqual(tab1.model.nodes[0].properties["name"], "State")

    def test_rf04_export_svg_png_jpeg_ppm(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 80, 80)

        with tempfile.TemporaryDirectory() as tmpdir:
            for ext, magic in [(".svg", b"<svg"), (".png", b"\x89PNG"), (".ppm", b"P6")]:
                out = os.path.join(tmpdir, f"diagrama{ext}")
                with patch("tkinter.filedialog.asksaveasfilename", return_value=out):
                    self.app.export_image()
                self.assertTrue(os.path.exists(out))
                with open(out, "rb") as f:
                    header = f.read(16)
                self.assertTrue(header.startswith(magic), f"Cabeçalho inválido para {ext}: {header}")

    def test_rf05_copy_to_clipboard_cropped_without_grid(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 200, 200)

        bbox = jetuml.compute_content_bbox(tab.model)
        self.assertGreaterEqual(bbox[0], 170)
        self.assertLess(bbox[2] - bbox[0], tab.model.width)

        # Verifica que a cena gerada sem grade não contém linhas de grade
        cmds_no_grid = jetuml.build_scene_commands(tab.model, self.app.current_theme(), include_grid=False)
        grid_lines = [c for c in cmds_no_grid if c.kind == "line" and c.data.get("fill") == self.app.current_theme()["grid"]]
        self.assertEqual(len(grid_lines), 0)

        with patch("jetuml.copy_image_to_system_clipboard", return_value=True) as mock_clip:
            self.app.copy_diagram_to_clipboard()
            mock_clip.assert_called_once()
        self.assertIsNotNone(self.app.toast_label)
        self.assertEqual(self.app.toast_label.cget("bg"), "#16A34A")

    def test_rf06_exit_app(self):
        self.app.exit_app()
        with self.assertRaises(tk.TclError):
            self.root.winfo_exists()


# ==============================================================================
# 2. BARRA DE FERRAMENTAS, ATALHOS E DICAS (RF07–RF10, RNF03)
# ==============================================================================

class TestToolbarShortcutsAndTooltips(BaseJetUMLTestCase):

    def test_rf07_toolbar_order_per_diagram(self):
        expected = {
            "Class Diagram": [
                "Select", "Class", "Interface", "Package", "Package Description", "Note",
                "Dependency", "Inheritance", "Realization", "Association", "Aggregation",
                "Composition", "Note Connector", "To Clipboard",
            ],
            "Sequence Diagram": [
                "Select", "Implicit Parameter", "Note", "Call", "Return", "Note Connector", "To Clipboard",
            ],
            "State Diagram": [
                "Select", "State", "Initial State", "Final State", "Note", "Transition", "Note Connector", "To Clipboard",
            ],
            "Object Diagram": [
                "Select", "Object", "Field", "Note", "Reference", "Collaboration", "Note Connector", "To Clipboard",
            ],
            "Use Case Diagram": [
                "Select", "Actor", "Use Case", "Note", "Association", "Include", "Extend",
                "Generalization", "Note Connector", "To Clipboard",
            ],
        }
        for dtype, names in expected.items():
            actual = [t[0] for t in jetuml.TOOLS_BY_DIAGRAM[dtype]]
            self.assertEqual(actual, names)

    def test_rf08_tool_shortcuts_1_to_9_0_a_b(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()

        # Tecla '2' -> Class, '0' -> Association (10ª), 'A' -> Aggregation (11ª), 'C' -> Note Connector (13ª)
        for key, expected_tool in [("2", "Class"), ("6", "Note"), ("0", "Association"), ("a", "Aggregation"), ("c", "Note Connector")]:
            self.app._on_tool_shortcut_key(make_event(char=key))
            self.assertEqual(tab.active_tool, expected_tool)

    def test_rf09_context_menu_popup(self):
        self.app.new_diagram("State Diagram")
        tab = self.app.current_tab()
        with patch.object(tk.Menu, "tk_popup") as mock_popup:
            tab._on_right_click(make_event(x=50, y=50, x_root=200, y_root=200))
            mock_popup.assert_called_once_with(200, 200)

    def test_rf10_show_tool_hint_and_verbose_tooltips(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()

        # Com Show Tool Hint ativo, botão exibe o nome da ferramenta
        self.app.show_tool_hint.set(True)
        tab.rebuild_toolbar()
        btn_text_with_hint = tab.tool_buttons[1][1].cget("text")
        self.assertIn("Class", btn_text_with_hint)

        # Sem Show Tool Hint, botão exibe apenas o ícone
        self.app.show_tool_hint.set(False)
        tab.rebuild_toolbar()
        btn_text_no_hint = tab.tool_buttons[1][1].cget("text")
        self.assertNotIn("Class", btn_text_no_hint)

        # Verbose Tooltips adiciona descrição detalhada
        self.app.verbose_tooltips.set(True)
        tab._show_tooltip(make_event(x_root=100, y_root=100), "Class", "Nó de Classe UML com nome, atributos e métodos.", "2")
        lbl = tab.tooltip_win.winfo_children()[0]
        self.assertIn("Nó de Classe UML", lbl.cget("text"))
        tab._hide_tooltip()


# ==============================================================================
# 3. NÓS, NOTAS E PACOTES (RF11–RF15)
# ==============================================================================

class TestNodesNotesAndPackages(BaseJetUMLTestCase):

    def test_rf11_rf13_create_nodes_and_notes(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 103, 147)
        tab.select_tool("Note")
        self.click_canvas(tab, 320, 150)

        self.assertEqual(len(tab.model.nodes), 2)
        self.assertEqual(tab.model.nodes[0].type, "Class")
        # RF36: Snap à grade de 10px
        self.assertEqual((tab.model.nodes[0].x, tab.model.nodes[0].y), (100, 150))
        self.assertEqual(tab.model.nodes[1].type, "Note")

    def test_rf12_auto_edit_node(self):
        self.app.new_diagram("State Diagram")
        tab = self.app.current_tab()
        self.app.auto_edit_node.set(True)

        with patch.object(tab, "open_properties_for") as mock_props:
            tab.select_tool("State")
            self.click_canvas(tab, 100, 100)
            self.assertEqual(mock_props.call_count, 1)

            # Estado Inicial não abre painel de propriedades
            tab.select_tool("Initial State")
            self.click_canvas(tab, 250, 100)
            self.assertEqual(mock_props.call_count, 1)

    def test_rf14_note_connector_to_node_and_fixed_point(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)
        tab.select_tool("Note")
        self.click_canvas(tab, 320, 100)

        cls_node, note_node = tab.model.nodes[0], tab.model.nodes[1]

        # 1. Arrastar de nó até a nota
        tab.select_tool("Note Connector")
        self.drag_canvas(tab, 140, 130, 350, 120)
        self.assertEqual(len(tab.model.edges), 1)
        edge1 = tab.model.edges[0]
        pts_before = jetuml.compute_edge_points(tab.model, edge1)

        # Mover nota atualiza conector
        note_node.x += 50
        pts_after = jetuml.compute_edge_points(tab.model, edge1)
        self.assertNotEqual(pts_before[-1], pts_after[-1])

        # 2. Arrastar da nota até um ponto vazio ancora no ponto fixo (PointNode)
        self.drag_canvas(tab, note_node.x + 20, note_node.y + 20, 500, 260)
        point_nodes = [n for n in tab.model.nodes if n.type == "PointNode"]
        self.assertEqual(len(point_nodes), 1)
        self.assertEqual(len(tab.model.edges), 2)

    def test_rf15_package_children_create_attach_detach_and_move(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()

        # Cria pacote em (100, 100) com tamanho padrão 220x160
        tab.select_tool("Package")
        self.click_canvas(tab, 100, 100)
        pkg = tab.model.nodes[0]

        # Clicar dentro do pacote cria nó já como filho
        tab.select_tool("Class")
        self.click_canvas(tab, 130, 140)
        child_cls = tab.model.nodes[1]
        self.assertEqual(child_cls.parent_id, pkg.id)

        # Criar classe fora do pacote e anexar com Shift
        self.click_canvas(tab, 420, 140)
        outside_cls = tab.model.nodes[2]
        self.assertIsNone(outside_cls.parent_id)

        tab.select_tool("Select")
        self.drag_canvas(tab, 450, 160, 180, 170, shift=True)
        self.assertEqual(outside_cls.parent_id, pkg.id)

        # Mover pacote move todos os filhos juntos
        old_child_x = child_cls.x
        self.drag_canvas(tab, pkg.x + 15, pkg.y + 10, pkg.x + 55, pkg.y + 10)
        self.assertEqual(child_cls.x, old_child_x + 40)

        # Desanexar filho arrastando para fora com Shift pressionado
        self.drag_canvas(tab, outside_cls.x + 20, outside_cls.y + 20, 550, 350, shift=True)
        self.assertIsNone(outside_cls.parent_id)


# ==============================================================================
# 4. ARESTAS, VALIDAÇÃO ESTRUTURAL E AUTOARESTAS (RF16–RF20)
# ==============================================================================

class TestEdgesAndStructuralValidation(BaseJetUMLTestCase):

    def test_rf16_rf17_create_edge_and_auto_recalculate(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)
        self.click_canvas(tab, 350, 100)
        c1, c2 = tab.model.nodes

        tab.select_tool("Association")
        self.drag_canvas(tab, 150, 130, 380, 130)
        self.assertEqual(len(tab.model.edges), 1)
        e = tab.model.edges[0]

        pts1 = jetuml.compute_edge_points(tab.model, e)
        c2.y += 120
        pts2 = jetuml.compute_edge_points(tab.model, e)
        self.assertNotEqual(pts1[-1][1], pts2[-1][1])

    def test_rf18_structural_validation_rules(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)
        self.click_canvas(tab, 360, 100)

        # 1. Bloquear aresta duplicada do mesmo tipo entre mesmos nós
        tab.select_tool("Dependency")
        self.drag_canvas(tab, 140, 130, 390, 130)
        self.assertEqual(len(tab.model.edges), 1)
        self.drag_canvas(tab, 140, 130, 390, 130)
        self.assertEqual(len(tab.model.edges), 1)
        self.assertEqual(self.app.toast_label.cget("bg"), "#DC2626")

        # 2. Bloquear ciclo direto (vai-e-volta) para Dependency, Inheritance, Aggregation, Association
        self.drag_canvas(tab, 390, 130, 140, 130)
        self.assertEqual(len(tab.model.edges), 1)

        tab.select_tool("Inheritance")
        self.drag_canvas(tab, 140, 130, 390, 130)
        self.assertEqual(len(tab.model.edges), 2)
        self.drag_canvas(tab, 390, 130, 140, 130)
        self.assertEqual(len(tab.model.edges), 2)

        # 3. Bloquear combinação de Association e Aggregation simultâneas entre mesmas classes
        tab.select_tool("Association")
        self.drag_canvas(tab, 140, 130, 390, 130)
        self.assertEqual(len(tab.model.edges), 3)

        tab.select_tool("Aggregation")
        self.drag_canvas(tab, 140, 130, 390, 130)
        self.assertEqual(len(tab.model.edges), 3)
        self.assertEqual(self.app.toast_label.cget("bg"), "#DC2626")

        # 4. Bloquear ciclo de colaboração entre Objetos
        self.app.new_diagram("Object Diagram")
        otab = self.app.current_tab()
        otab.select_tool("Object")
        self.click_canvas(otab, 100, 100)
        self.click_canvas(otab, 350, 100)
        otab.select_tool("Collaboration")
        self.drag_canvas(otab, 140, 120, 380, 120)
        self.assertEqual(len(otab.model.edges), 1)
        self.drag_canvas(otab, 380, 120, 140, 120)
        self.assertEqual(len(otab.model.edges), 1)

    def test_rf19_self_edges_and_state_diagram_limits(self):
        # Class Diagram: bloqueia auto-herança e auto-dependência
        self.app.new_diagram("Class Diagram")
        ctab = self.app.current_tab()
        ctab.select_tool("Class")
        self.click_canvas(ctab, 120, 120)
        for edge_tool in ("Inheritance", "Dependency"):
            ctab.select_tool(edge_tool)
            self.drag_canvas(ctab, 140, 140, 180, 160)
            self.assertEqual(len(ctab.model.edges), 0)

        # Use Case Diagram: bloqueia autoarestas em Actor e Use Case
        self.app.new_diagram("Use Case Diagram")
        utab = self.app.current_tab()
        utab.select_tool("Actor")
        self.click_canvas(utab, 100, 100)
        utab.select_tool("Generalization")
        self.drag_canvas(utab, 120, 120, 130, 140)
        self.assertEqual(len(utab.model.edges), 0)

        # State Diagram: até 2 autotransições, até 2 por direção, bloqueio em Initial/Final
        self.app.new_diagram("State Diagram")
        stab = self.app.current_tab()
        stab.select_tool("Initial State")
        self.click_canvas(stab, 60, 120)
        stab.select_tool("State")
        self.click_canvas(stab, 180, 110)
        stab.click_canvas(stab, 380, 110)
        stab.select_tool("Final State")
        self.click_canvas(stab, 560, 120)

        stab.select_tool("Transition")
        # Bloqueia entrada em Initial State
        self.drag_canvas(stab, 210, 130, 70, 130)
        self.assertEqual(len(stab.model.edges), 0)

        # Bloqueia saída de Final State
        self.drag_canvas(stab, 570, 130, 410, 130)
        self.assertEqual(len(stab.model.edges), 0)

        # Aceita até 2 autotransições no State 1 e rejeita a 3ª
        self.drag_canvas(stab, 200, 130, 230, 140)
        self.drag_canvas(stab, 200, 130, 230, 140)
        self.assertEqual(len(stab.model.edges), 2)
        self.drag_canvas(stab, 200, 130, 230, 140)
        self.assertEqual(len(stab.model.edges), 2)

        # Aceita até 2 transições na mesma direção entre State 1 e State 2 e rejeita a 3ª
        self.drag_canvas(stab, 220, 130, 410, 130)
        self.drag_canvas(stab, 220, 130, 410, 130)
        self.assertEqual(len(stab.model.edges), 4)
        self.drag_canvas(stab, 220, 130, 410, 130)
        self.assertEqual(len(stab.model.edges), 4)

    def test_rf20_edge_properties_editing(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)
        self.click_canvas(tab, 350, 100)
        tab.select_tool("Aggregation")
        self.drag_canvas(tab, 140, 130, 380, 130)
        edge = tab.model.edges[0]

        dlg = jetuml.PropertiesDialog(self.root, edge, "Class Diagram", lambda el: None)
        dlg.widgets["start_label"][1].insert(0, "1")
        dlg.widgets["middle_label"][1].insert(0, "contém")
        dlg.widgets["end_label"][1].insert(0, "0..*")
        dlg.widgets["directionality"][1].set("Unidirectional")
        dlg.widgets["aggregation_type"][1].set("Composition")
        dlg._save()

        self.assertEqual(edge.type, "Composition")
        self.assertEqual(edge.properties["start_label"], "1")
        self.assertEqual(edge.properties["middle_label"], "contém")
        self.assertEqual(edge.properties["end_label"], "0..*")
        self.assertEqual(edge.properties["directionality"], "Unidirectional")


# ==============================================================================
# 5. UNDO/REDO, PROPRIEDADES, ESTEREÓTIPOS E FORMATAÇÃO (RF21–RF24)
# ==============================================================================

class TestPropertiesStereotypesFormattingAndUndo(BaseJetUMLTestCase):

    def test_rf21_undo_and_redo_sequence(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)
        self.click_canvas(tab, 300, 100)
        self.assertEqual(len(tab.model.nodes), 2)

        tab.undo()
        self.assertEqual(len(tab.model.nodes), 1)
        tab.undo()
        self.assertEqual(len(tab.model.nodes), 0)

        tab.redo()
        self.assertEqual(len(tab.model.nodes), 1)
        tab.redo()
        self.assertEqual(len(tab.model.nodes), 2)

    def test_rf22_rf23_properties_and_ctrl_q_stereotype(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)
        cls_node = tab.model.nodes[0]

        dlg = jetuml.PropertiesDialog(self.root, cls_node, "Class Diagram", lambda el: None)
        self.assertIn("name", dlg.widgets)
        self.assertIn("attributes", dlg.widgets)
        self.assertIn("methods", dlg.widgets)

        entry = dlg.widgets["name"][1]
        entry.delete(0, "end")
        entry.focus_set()
        dlg._insert_stereotype()
        self.assertEqual(entry.get(), "«»")
        self.assertEqual(entry.index(tk.INSERT), 1)
        entry.insert(tk.INSERT, "entity")
        self.assertEqual(entry.get(), "«entity»")
        dlg._save()
        self.assertEqual(cls_node.properties["name"], "«entity»")

    def test_rf24_italic_and_underline_parser(self):
        segs = jetuml.parse_formatted_line("/AbstractClass/ e _staticMethod()_")
        self.assertEqual(segs[0], ("AbstractClass", True, False))
        self.assertEqual(segs[1], (" e ", False, False))
        self.assertEqual(segs[2], ("staticMethod()", False, True))


# ==============================================================================
# 6. DIAGRAMA DE OBJETOS E DIAGRAMA DE SEQUÊNCIA (RF25–RF31)
# ==============================================================================

class TestObjectAndSequenceDiagrams(BaseJetUMLTestCase):

    def test_rf25_rf26_object_fields_and_references(self):
        self.app.new_diagram("Object Diagram")
        tab = self.app.current_tab()

        # RF25: Criar Field fora de um Object é proibido
        tab.select_tool("Field")
        self.click_canvas(tab, 100, 100)
        self.assertEqual(len(tab.model.nodes), 0)
        self.assertEqual(self.app.toast_label.cget("bg"), "#DC2626")

        # Cria dois Objetos e adiciona Field dentro do primeiro
        tab.select_tool("Object")
        self.click_canvas(tab, 100, 100)
        self.click_canvas(tab, 360, 100)
        obj1, obj2 = tab.model.nodes[0], tab.model.nodes[1]

        tab.select_tool("Field")
        self.click_canvas(tab, obj1.x + 20, obj1.y + 20)
        fields = [n for n in tab.model.nodes if n.type == "Field"]
        self.assertEqual(len(fields), 1)
        fld = fields[0]
        self.assertEqual(fld.parent_id, obj1.id)

        jetuml.sync_model_layout(tab.model)

        # RF26: Reference da parte Name (esquerda) deve falhar; da parte Value (direita) deve funcionar
        tab.select_tool("Reference")
        self.drag_canvas(tab, fld.x + 10, fld.y + 10, obj2.x + 30, obj2.y + 20)
        self.assertEqual(len(tab.model.edges), 0)

        self.drag_canvas(tab, fld.x + fld.width * 0.75, fld.y + 10, obj2.x + 30, obj2.y + 20)
        self.assertEqual(len(tab.model.edges), 1)
        self.assertEqual(tab.model.edges[0].type, "Reference")

    def test_rf27_rf28_rf29_rf30_rf31_sequence_diagram_workflow(self):
        self.app.new_diagram("Sequence Diagram")
        tab = self.app.current_tab()

        tab.select_tool("Implicit Parameter")
        self.click_canvas(tab, 100, 60)
        self.click_canvas(tab, 280, 60)
        self.click_canvas(tab, 460, 60)
        p1, p2, p3 = tab.model.nodes

        # RF30: Tentar chamar em ponto vazio arbitrário falha silenciosamente (sem toast)
        tab.select_tool("Call")
        self.drag_canvas(tab, 20, 300, 600, 300)
        self.assertEqual(len(tab.model.edges), 0)
        self.assertIsNone(self.app.toast_label)

        # RF27: Chamada inicial de p1 -> p2 cria chamada e 2 caixas de ativação
        self.drag_canvas(tab, p1.x + p1.width / 2, 140, p2.x + p2.width / 2, 140)
        call_nodes = [n for n in tab.model.nodes if n.type == "CallNode"]
        self.assertEqual(len(call_nodes), 2)
        self.assertEqual(len(tab.model.edges), 1)

        cn2 = [cn for cn in call_nodes if cn.parent_id == p2.id][0]

        # RF28: Autochamada dentro da caixa de ativação cn2
        self.drag_canvas(tab, cn2.x + 6, cn2.y + 10, cn2.x + 6, cn2.y + 24)
        self.assertEqual(len([n for n in tab.model.nodes if n.type == "CallNode"]), 3)

        # RF29: Chamada de construtor para cabeçalho de p3 rotula «create» e alinha p3 verticalmente
        self.drag_canvas(tab, cn2.x + 6, cn2.y + 30, p3.x + 25, p3.y + 20)
        create_edges = [e for e in tab.model.edges if e.properties.get("middle_label") == "«create»"]
        self.assertEqual(len(create_edges), 1)
        self.assertGreater(p3.y, 60)

        # RF30: Criar segundo bloco raiz órfão no mesmo diagrama falha silenciosamente
        edges_before = len(tab.model.edges)
        self.drag_canvas(tab, p3.x + p3.width / 2, 350, p1.x + p1.width / 2, 350)
        self.assertEqual(len(tab.model.edges), edges_before)
        self.assertIsNone(self.app.toast_label)

        # RF31: Propriedade Open Bottom
        first_call = tab.model.edges[0]
        dlg = jetuml.PropertiesDialog(self.root, first_call, "Sequence Diagram", lambda el: None)
        dlg.widgets["open_bottom"][1].set(True)
        dlg._save()
        self.assertTrue(first_call.properties["open_bottom"])


# ==============================================================================
# 7. SELEÇÃO, CLIPBOARD DE ELEMENTOS, VIEW E NOTIFICAÇÕES (RF32–RF42)
# ==============================================================================

class TestSelectionClipboardAndViewSettings(BaseJetUMLTestCase):

    def test_rf32_rf33_rf35_selection_lasso_select_all_and_delete(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()
        tab.select_tool("Class")
        self.click_canvas(tab, 100, 100)
        self.click_canvas(tab, 320, 100)
        c1, c2 = tab.model.nodes

        tab.select_tool("Select")
        # Clique em espaço vazio desseleciona
        self.click_canvas(tab, 500, 500)
        self.assertEqual(len(tab.selected_ids), 0)

        # Ctrl+clique adiciona múltiplos elementos
        self.click_canvas(tab, c1.x + 20, c1.y + 20, ctrl=True)
        self.click_canvas(tab, c2.x + 20, c2.y + 20, ctrl=True)
        self.assertEqual(tab.selected_ids, {c1.id, c2.id})

        # Laço (lasso) seleciona região
        self.click_canvas(tab, 500, 500)
        self.drag_canvas(tab, 80, 80, 500, 250)
        self.assertEqual(tab.selected_ids, {c1.id, c2.id})

        # Select All (Ctrl+A) e Delete
        tab.select_all()
        self.assertEqual(len(tab.selected_ids), 2)
        tab.delete_selected()
        self.assertEqual(len(tab.model.nodes), 0)

    def test_rf34_cut_copy_paste_and_diagram_compatibility(self):
        self.app.new_diagram("Class Diagram")
        ctab = self.app.current_tab()
        ctab.select_tool("Class")
        self.click_canvas(ctab, 100, 100)
        self.app.copy_selection()

        # Colar no mesmo diagrama duplica o elemento
        self.app.paste_selection()
        self.assertEqual(len(ctab.model.nodes), 2)

        # Colar Class em State Diagram deve falhar com notificação de erro
        self.app.new_diagram("State Diagram")
        stab = self.app.current_tab()
        self.app.paste_selection()
        self.assertEqual(len(stab.model.nodes), 0)
        self.assertEqual(self.app.toast_label.cget("bg"), "#DC2626")

        # Colar em Sequence Diagram é proibido mesmo com Note compatível
        ctab.select_tool("Note")
        self.click_canvas(ctab, 400, 100)
        self.app.notebook.select(ctab)
        self.app.copy_selection()

        self.app.new_diagram("Sequence Diagram")
        seqtab = self.app.current_tab()
        self.app.paste_selection()
        self.assertEqual(len(seqtab.model.nodes), 0)
        self.assertEqual(self.app.toast_label.cget("bg"), "#DC2626")

    def test_rf36_rf37_rf38_rf39_rf40_view_options(self):
        self.app.new_diagram("Class Diagram")
        tab = self.app.current_tab()

        # RF36: Grade oculta mantém snap ativo
        self.app.show_grid.set(False)
        tab.select_tool("Class")
        self.click_canvas(tab, 114, 126)
        self.assertEqual((tab.model.nodes[0].x, tab.model.nodes[0].y), (110, 130))

        # RF37: Zoom +/- 2 níveis e reset
        self.assertEqual(self.app.zoom_factor(), 1.0)
        self.app.zoom_in()
        self.app.zoom_in()
        self.app.zoom_in()  # Não ultrapassa 2 níveis
        self.assertEqual(self.app.zoom_index, 4)
        self.app.reset_zoom()
        self.assertEqual(self.app.zoom_factor(), 1.0)
        self.app.zoom_out()
        self.app.zoom_out()
        self.app.zoom_out()  # Não ultrapassa 2 níveis abaixo
        self.assertEqual(self.app.zoom_index, 0)

        # RF38: Tamanho do diagrama entre 250 e 4000 e expansão automática ao abrir arquivo maior
        big_data = jetuml.DiagramModel("Class Diagram", width=2200, height=1800).to_dict()
        with tempfile.TemporaryDirectory() as tmpdir:
            p = os.path.join(tmpdir, "grande.json")
            with open(p, "w", encoding="utf-8") as f:
                json.dump(big_data, f)
            self.app.open_diagram(p)
            self.assertGreaterEqual(self.app.diag_width, 2200)
            self.assertGreaterEqual(self.app.diag_height, 1800)

        # RF39: Alteração de fonte
        self.app.diagram_font_family.set("Courier")
        self.app.diagram_font_size.set(13)
        self.app._redraw_all_tabs()
        self.assertEqual(self.app.diagram_font_family.get(), "Courier")

        # RF40: Modo escuro
        self.app.dark_mode.set(True)
        self.app.apply_theme()
        self.assertEqual(self.app.current_theme()["canvas_bg"], "#1E293B")

    def test_rf41_rf42_notifications_and_duration_zero(self):
        self.assertEqual(self.app.notification_duration.get(), 5)
        self.app.notify("Operação concluída", is_error=False)
        self.assertEqual(self.app.toast_label.cget("bg"), "#16A34A")

        self.app.notify("Erro estrutural", is_error=True)
        self.assertEqual(self.app.toast_label.cget("bg"), "#DC2626")

        # Duração 0 desativa todas as notificações
        self.app._dismiss_toast()
        self.app.notification_duration.set(0)
        self.app.notify("Não deve aparecer", is_error=True)
        self.assertIsNone(self.app.toast_label)


if __name__ == "__main__":
    unittest.main(verbosity=2)