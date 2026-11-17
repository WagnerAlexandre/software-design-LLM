#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JetUML Python Edition
Implementação completa dos requisitos funcionais RF01–RF42 e RNF01–RNF03.
"""

import copy
import json
import math
import os
import platform
import re
import struct
import subprocess
import sys
import tempfile
import uuid
import zlib
import tkinter as tk
from tkinter import ttk, filedialog, messagebox, font as tkfont

try:
    from PIL import Image, ImageDraw, ImageFont
    HAS_PIL = True
except ImportError:
    HAS_PIL = False


# ==============================================================================
# 1. CONSTANTES, TEMAS E DEFINIÇÃO DE FERRAMENTAS (RF07, RF08, RF10, RF37, RF40)
# ==============================================================================

APP_TITLE = "JetUML — Python Edition"
RECENT_FILES_PATH = os.path.join(os.path.expanduser("~"), ".jetuml_py_recent.json")
GRID_SIZE = 10
ZOOM_LEVELS = [0.70, 0.85, 1.00, 1.20, 1.45]  # 2 níveis abaixo, padrão, 2 acima (RF37)

DIAGRAM_TYPES = [
    "Class Diagram",
    "Sequence Diagram",
    "State Diagram",
    "Object Diagram",
    "Use Case Diagram",
]

# Definição exata das ferramentas por tipo de diagrama (RF07)
# Cada item: (nome, categoria, icone_ascii, dica_curta, dica_detalhada)
TOOLS_BY_DIAGRAM = {
    "Class Diagram": [
        ("Select", "select", "↖", "Select", "Seleciona, move e redimensiona elementos ou abre laço de seleção."),
        ("Class", "node", "▭", "Class", "Nó de Classe UML com nome, atributos e métodos."),
        ("Interface", "node", "«I»", "Interface", "Nó de Interface UML com estereótipo «interface» e métodos."),
        ("Package", "node", "📁", "Package", "Pacote UML capaz de agrupar nós filhos (use Shift para anexar/desanexar)."),
        ("Package Description", "node", "🗂", "Package Description", "Pacote UML com área textual de descrição/conteúdo."),
        ("Note", "node", "📝", "Note", "Nota explicativa para comentários livres no diagrama."),
        ("Dependency", "edge", "⇢", "Dependency", "Relação de dependência tracejada entre elementos."),
        ("Inheritance", "edge", "▷", "Inheritance", "Herança/Generalização (seta com triângulo vazio)."),
        ("Realization", "edge", "⇢▷", "Realization", "Realização de interface (linha tracejada com triângulo vazio)."),
        ("Association", "edge", "—", "Association", "Associação estrutural entre classes."),
        ("Aggregation", "edge", "◇—", "Aggregation", "Agregação compartilhada (losango vazio na origem)."),
        ("Composition", "edge", "◆—", "Composition", "Composição exclusiva (losango preenchido na origem)."),
        ("Note Connector", "edge", "┄", "Note Connector", "Conecta uma Nota a um nó ou a um ponto fixo no diagrama."),
        ("To Clipboard", "action", "📋", "To Clipboard", "Copia a imagem do diagrama (sem grade) para a área de transferência."),
    ],
    "Sequence Diagram": [
        ("Select", "select", "↖", "Select", "Seleciona e move elementos no diagrama de sequência."),
        ("Implicit Parameter", "node", "▯", "Implicit Parameter", "Participante/objeto com linha de vida vertical tracejada."),
        ("Note", "node", "📝", "Note", "Nota explicativa vinculável a elementos ou pontos."),
        ("Call", "edge", "→", "Call", "Chamada síncrona, autochamada ou chamada de construtor «create»."),
        ("Return", "edge", "⇠", "Return", "Retorno de controle tracejado entre caixas de ativação."),
        ("Note Connector", "edge", "┄", "Note Connector", "Conecta uma Nota a um elemento ou ponto fixo."),
        ("To Clipboard", "action", "📋", "To Clipboard", "Copia a imagem do diagrama (sem grade) para a área de transferência."),
    ],
    "State Diagram": [
        ("Select", "select", "↖", "Select", "Seleciona e move estados e notas."),
        ("State", "node", "▢", "State", "Estado UML com cantos arredondados e nome editável."),
        ("Initial State", "node", "●", "Initial State", "Pseudoestado inicial (círculo sólido). Não aceita transições de entrada."),
        ("Final State", "node", "◉", "Final State", "Estado final (círculo duplo). Não aceita transições de saída."),
        ("Note", "node", "📝", "Note", "Nota explicativa para anotações no diagrama de estados."),
        ("Transition", "edge", "→", "Transition", "Transição direcionada entre estados (até 2 por direção ou 2 autotransições)."),
        ("Note Connector", "edge", "┄", "Note Connector", "Conecta uma Nota a um estado ou ponto fixo."),
        ("To Clipboard", "action", "📋", "To Clipboard", "Copia a imagem do diagrama (sem grade) para a área de transferência."),
    ],
    "Object Diagram": [
        ("Select", "select", "↖", "Select", "Seleciona e move objetos, campos e notas."),
        ("Object", "node", "▭", "Object", "Instância de objeto UML com nome sublinhado."),
        ("Field", "node", "a=b", "Field", "Campo (nome = valor) pertencente obrigatoriamente a um Objeto."),
        ("Note", "node", "📝", "Note", "Nota explicativa no diagrama de objetos."),
        ("Reference", "edge", "•→", "Reference", "Referência da área de valor de um Campo até um Objeto de destino."),
        ("Collaboration", "edge", "—", "Collaboration", "Ligação de colaboração entre dois Objetos (sem ciclos diretos)."),
        ("Note Connector", "edge", "┄", "Note Connector", "Conecta uma Nota a um elemento ou ponto fixo."),
        ("To Clipboard", "action", "📋", "To Clipboard", "Copia a imagem do diagrama (sem grade) para a área de transferência."),
    ],
    "Use Case Diagram": [
        ("Select", "select", "↖", "Select", "Seleciona e move atores, casos de uso e notas."),
        ("Actor", "node", "🏃", "Actor", "Ator externo que interage com o sistema."),
        ("Use Case", "node", "⬭", "Use Case", "Caso de uso representado por elipse com nome."),
        ("Note", "node", "📝", "Note", "Nota explicativa no diagrama de casos de uso."),
        ("Association", "edge", "—", "Association", "Associação entre Ator e Caso de Uso."),
        ("Include", "edge", "«inc»", "Include", "Dependência «include» entre casos de uso."),
        ("Extend", "edge", "«ext»", "Extend", "Dependência «extend» entre casos de uso."),
        ("Generalization", "edge", "▷", "Generalization", "Generalização entre Atores ou entre Casos de Uso."),
        ("Note Connector", "edge", "┄", "Note Connector", "Conecta uma Nota a um elemento ou ponto fixo."),
        ("To Clipboard", "action", "📋", "To Clipboard", "Copia a imagem do diagrama (sem grade) para a área de transferência."),
    ],
}

SHORTCUT_KEYS = [
    "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
    "A", "B", "C", "D", "E", "F", "G", "H"
]

NODE_COMPATIBILITY = {
    "Class Diagram": {"Class", "Interface", "Package", "Package Description", "Note", "PointNode"},
    "Sequence Diagram": {"Implicit Parameter", "CallNode", "Note", "PointNode"},
    "State Diagram": {"State", "Initial State", "Final State", "Note", "PointNode"},
    "Object Diagram": {"Object", "Field", "Note", "PointNode"},
    "Use Case Diagram": {"Actor", "Use Case", "Note", "PointNode"},
}

EDGE_COMPATIBILITY = {
    "Class Diagram": {"Dependency", "Inheritance", "Realization", "Association", "Aggregation", "Composition", "Note Connector"},
    "Sequence Diagram": {"Call", "Return", "Note Connector"},
    "State Diagram": {"Transition", "Note Connector"},
    "Object Diagram": {"Reference", "Collaboration", "Note Connector"},
    "Use Case Diagram": {"Association", "Include", "Extend", "Generalization", "Note Connector"},
}

THEMES = {
    "light": {
        "bg": "#F3F4F6",
        "panel_bg": "#FFFFFF",
        "toolbar_bg": "#E5E7EB",
        "tool_active": "#DBEAFE",
        "tool_border": "#2563EB",
        "canvas_outer": "#D1D5DB",
        "canvas_bg": "#FFFFFF",
        "grid": "#E5E7EB",
        "fg": "#111827",
        "muted_fg": "#4B5563",
        "node_fill": "#FFFBEB",
        "node_header": "#FEF3C7",
        "note_fill": "#FEF9C3",
        "package_fill": "#F8FAFC",
        "stroke": "#1F2937",
        "select": "#2563EB",
        "rubber": "#2563EB",
        "package_highlight": "#10B981",
        "accent": "#2563EB",
        "accent_fg": "#FFFFFF",
    },
    "dark": {
        "bg": "#111827",
        "panel_bg": "#1F2937",
        "toolbar_bg": "#1E293B",
        "tool_active": "#1D4ED8",
        "tool_border": "#60A5FA",
        "canvas_outer": "#0F172A",
        "canvas_bg": "#1E293B",
        "grid": "#334155",
        "fg": "#F9FAFB",
        "muted_fg": "#9CA3AF",
        "node_fill": "#273549",
        "node_header": "#334155",
        "note_fill": "#3F3A22",
        "package_fill": "#1B2638",
        "stroke": "#E5E7EB",
        "select": "#60A5FA",
        "rubber": "#60A5FA",
        "package_highlight": "#34D399",
        "accent": "#3B82F6",
        "accent_fg": "#FFFFFF",
    },
}


# ==============================================================================
# 2. MODELO DE DADOS & SERIALIZAÇÃO JSON (RF01, RF02, RNF02)
# ==============================================================================

def snap_val(v: float) -> int:
    return int(round(v / GRID_SIZE) * GRID_SIZE)


class DiagramNode:
    """Representa qualquer nó UML no diagrama."""

    def __init__(self, node_type: str, x: float, y: float, node_id: str = None, **kwargs):
        self.id = node_id or str(uuid.uuid4())
        self.type = node_type
        self.x = float(x)
        self.y = float(y)
        self.width = float(kwargs.get("width", self._default_width(node_type)))
        self.height = float(kwargs.get("height", self._default_height(node_type)))
        self.parent_id = kwargs.get("parent_id", None)
        self.properties = kwargs.get("properties", self._default_properties(node_type))

    @staticmethod
    def _default_width(t: str) -> int:
        return {
            "Class": 140,
            "Interface": 140,
            "Package": 220,
            "Package Description": 180,
            "Note": 130,
            "Implicit Parameter": 110,
            "CallNode": 16,
            "State": 110,
            "Initial State": 24,
            "Final State": 28,
            "Object": 140,
            "Field": 120,
            "Actor": 60,
            "Use Case": 130,
            "PointNode": 8,
        }.get(t, 120)

    @staticmethod
    def _default_height(t: str) -> int:
        return {
            "Class": 90,
            "Interface": 80,
            "Package": 160,
            "Package Description": 100,
            "Note": 65,
            "Implicit Parameter": 45,
            "CallNode": 40,
            "State": 55,
            "Initial State": 24,
            "Final State": 28,
            "Object": 60,
            "Field": 24,
            "Actor": 85,
            "Use Case": 60,
            "PointNode": 8,
        }.get(t, 60)

    @staticmethod
    def _default_properties(t: str) -> dict:
        if t == "Class":
            return {"name": "ClassName", "attributes": "", "methods": ""}
        if t == "Interface":
            return {"name": "InterfaceName", "methods": ""}
        if t == "Package":
            return {"name": "package"}
        if t == "Package Description":
            return {"name": "package", "contents": ""}
        if t == "Note":
            return {"text": "Note"}
        if t == "Implicit Parameter":
            return {"name": "object:Type"}
        if t == "CallNode":
            return {"open_bottom": False}
        if t == "State":
            return {"name": "State"}
        if t == "Object":
            return {"name": "object:Class"}
        if t == "Field":
            return {"name": "field", "value": "value"}
        if t == "Actor":
            return {"name": "Actor"}
        if t == "Use Case":
            return {"name": "Use Case"}
        return {}

    def bounds(self):
        return (self.x, self.y, self.x + self.width, self.y + self.height)

    def center(self):
        return (self.x + self.width / 2.0, self.y + self.height / 2.0)

    def contains(self, px: float, py: float) -> bool:
        x1, y1, x2, y2 = self.bounds()
        return x1 <= px <= x2 and y1 <= py <= y2

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "type": self.type,
            "x": self.x,
            "y": self.y,
            "width": self.width,
            "height": self.height,
            "parent_id": self.parent_id,
            "properties": copy.deepcopy(self.properties),
        }

    @classmethod
    def from_dict(cls, data: dict) -> "DiagramNode":
        return cls(
            node_type=data["type"],
            x=data["x"],
            y=data["y"],
            node_id=data["id"],
            width=data.get("width", cls._default_width(data["type"])),
            height=data.get("height", cls._default_height(data["type"])),
            parent_id=data.get("parent_id"),
            properties=copy.deepcopy(data.get("properties", {})),
        )


class DiagramEdge:
    """Representa qualquer aresta, transição, referência ou conector UML."""

    def __init__(self, edge_type: str, source_id: str, target_id: str, edge_id: str = None, **kwargs):
        self.id = edge_id or str(uuid.uuid4())
        self.type = edge_type
        self.source_id = source_id
        self.target_id = target_id
        self.properties = kwargs.get("properties", self._default_properties(edge_type))

    @staticmethod
    def _default_properties(t: str) -> dict:
        if t in ("Association", "Aggregation", "Composition"):
            props = {
                "start_label": "",
                "middle_label": "",
                "end_label": "",
                "directionality": "Unspecified",
            }
            if t in ("Aggregation", "Composition"):
                props["aggregation_type"] = t
            return props
        if t in ("Inheritance", "Realization", "Dependency"):
            gen_type = "Implementation" if t == "Realization" else "Inheritance"
            return {
                "generalization_type": gen_type,
                "directionality": "Unidirectional",
                "middle_label": "",
            }
        if t in ("Call", "Return", "Collaboration", "Transition"):
            props = {"middle_label": ""}
            if t == "Call":
                props["open_bottom"] = False
                props["is_create"] = False
            return props
        if t in ("Include", "Extend"):
            return {"dependency_type": t}
        return {}

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "type": self.type,
            "source_id": self.source_id,
            "target_id": self.target_id,
            "properties": copy.deepcopy(self.properties),
        }

    @classmethod
    def from_dict(cls, data: dict) -> "DiagramEdge":
        return cls(
            edge_type=data["type"],
            source_id=data["source_id"],
            target_id=data["target_id"],
            edge_id=data["id"],
            properties=copy.deepcopy(data.get("properties", {})),
        )


class DiagramModel:
    """Modelo completo de um diagrama UML serializável em JSON aberto (RNF02)."""

    def __init__(self, diagram_type: str, width: int = 1000, height: int = 700):
        self.diagram_type = diagram_type
        self.width = width
        self.height = height
        self.nodes: list[DiagramNode] = []
        self.edges: list[DiagramEdge] = []

    def get_node(self, node_id: str):
        for n in self.nodes:
            if n.id == node_id:
                return n
        return None

    def get_edge(self, edge_id: str):
        for e in self.edges:
            if e.id == edge_id:
                return e
        return None

    def children_of(self, parent_id: str) -> list[DiagramNode]:
        return [n for n in self.nodes if n.parent_id == parent_id]

    def to_dict(self) -> dict:
        return {
            "format": "JetUML-JSON-1.0",
            "diagram_type": self.diagram_type,
            "width": self.width,
            "height": self.height,
            "nodes": [n.to_dict() for n in self.nodes],
            "edges": [e.to_dict() for e in self.edges],
        }

    @classmethod
    def from_dict(cls, data: dict) -> "DiagramModel":
        dtype = data.get("diagram_type", "Class Diagram")
        if dtype not in DIAGRAM_TYPES:
            raise ValueError(f"Tipo de diagrama inválido: {dtype}")
        model = cls(
            diagram_type=dtype,
            width=int(data.get("width", 1000)),
            height=int(data.get("height", 700)),
        )
        model.nodes = [DiagramNode.from_dict(nd) for nd in data.get("nodes", [])]
        model.edges = [DiagramEdge.from_dict(ed) for ed in data.get("edges", [])]
        return model


# ==============================================================================
# 3. MOTOR DE VALIDAÇÃO ESTRUTURAL (RF18, RF19, RF25, RF26, RF30, RF34)
# ==============================================================================

class ValidationResult:
    def __init__(self, valid: bool, message: str = "", silent: bool = False):
        self.valid = valid
        self.message = message
        self.silent = silent


def validate_edge_creation(model: DiagramModel, edge_type: str, src: DiagramNode, dst: DiagramNode) -> ValidationResult:
    """Valida todas as regras de conexão conforme RF14, RF18, RF19, RF26, RF30."""
    dtype = model.diagram_type

    if src is None or dst is None:
        return ValidationResult(False, "Origem ou destino inválido.")

    # Regra geral de Note Connector (RF14)
    if edge_type == "Note Connector":
        if src.type != "Note" and dst.type != "Note":
            return ValidationResult(False, "O Note Connector deve ligar uma Nota a um elemento ou ponto.")
        if src.id == dst.id:
            return ValidationResult(False, "Não é possível conectar uma Nota a ela mesma.")
        # Evita duplicata exata
        for e in model.edges:
            if e.type == "Note Connector" and {e.source_id, e.target_id} == {src.id, dst.id}:
                return ValidationResult(False, "Já existe um conector entre esta Nota e o elemento.")
        return ValidationResult(True)

    # Regra de Referência em Diagrama de Objetos (RF26)
    if edge_type == "Reference":
        if src.type != "Field" or dst.type != "Object":
            return ValidationResult(False, "Referências devem partir do valor de um Campo até um Objeto.")
        return ValidationResult(True)

    # Regras específicas do Diagrama de Sequência (RF27-RF30)
    if dtype == "Sequence Diagram":
        # RF30: Falhas de fluxo de controle em pontos arbitrários falham silenciosamente
        if edge_type not in ("Call", "Return"):
            return ValidationResult(False, silent=True)
        valid_seq_targets = ("Implicit Parameter", "CallNode")
        if src.type not in valid_seq_targets or dst.type not in valid_seq_targets:
            return ValidationResult(False, silent=True)
        return ValidationResult(True)

    # A partir daqui, PointNode e Note não participam de arestas semânticas UML
    if src.type in ("Note", "PointNode") or dst.type in ("Note", "PointNode"):
        return ValidationResult(False, f"Aresta '{edge_type}' não pode conectar Notas ou pontos avulsos.")

    # Regras do Diagrama de Estados (RF19)
    if dtype == "State Diagram":
        if edge_type != "Transition":
            return ValidationResult(False, "Tipo de aresta inválido no Diagrama de Estados.")
        if src.type == "Final State":
            return ValidationResult(False, "Não é permitido criar transições partindo de um Estado Final.")
        if dst.type == "Initial State":
            return ValidationResult(False, "Não é permitido criar transições chegando em um Estado Inicial.")

        if src.id == dst.id:
            self_count = sum(
                1 for e in model.edges
                if e.type == "Transition" and e.source_id == src.id and e.target_id == dst.id
            )
            if self_count >= 2:
                return ValidationResult(False, "Cada estado aceita no máximo 2 autotransições.")
        else:
            dir_count = sum(
                1 for e in model.edges
                if e.type == "Transition" and e.source_id == src.id and e.target_id == dst.id
            )
            if dir_count >= 2:
                return ValidationResult(False, "Máximo de 2 transições na mesma direção entre dois estados.")
        return ValidationResult(True)

    # Regras de Autoarestas (RF19) fora de State Diagram
    if src.id == dst.id:
        if dtype == "Class Diagram" and edge_type in ("Inheritance", "Dependency", "Realization"):
            return ValidationResult(False, f"Não é permitido criar auto-{edge_type.lower()} em Diagramas de Classes.")
        if dtype == "Use Case Diagram":
            return ValidationResult(False, "Atores e Casos de Uso não permitem autoarestas.")
        if dtype == "Object Diagram":
            return ValidationResult(False, "Objetos não permitem autocolaboração.")

    # Regras estruturais RF18 (não-State Diagram)
    # 1. Não criar mais de 1 aresta do mesmo tipo entre os mesmos dois nós
    for e in model.edges:
        if e.type == edge_type and e.source_id == src.id and e.target_id == dst.id:
            return ValidationResult(False, f"Já existe uma aresta '{edge_type}' entre estes elementos.")

    # 2. Diagrama de Classes
    if dtype == "Class Diagram":
        if src.type == "Package" or dst.type == "Package":
            if edge_type != "Dependency":
                return ValidationResult(False, "Pacotes aceitam apenas relações de Dependência.")
        if edge_type == "Realization":
            if not (src.type == "Class" and dst.type == "Interface"):
                return ValidationResult(False, "Realização deve partir de uma Classe até uma Interface.")
        if edge_type == "Inheritance":
            if src.type != dst.type or src.type not in ("Class", "Interface"):
                return ValidationResult(False, "Herança exige dois elementos compatíveis (Classe->Classe ou Interface->Interface).")

        # Ciclos diretos proibidos (vai-e-volta) para Herança, Dependência, Agregação ou Associação
        cycle_types = {"Inheritance", "Dependency", "Aggregation", "Association"}
        if edge_type in cycle_types and src.id != dst.id:
            for e in model.edges:
                if e.type == edge_type and e.source_id == dst.id and e.target_id == src.id:
                    return ValidationResult(False, f"Ciclo direto proibido para '{edge_type}' entre as mesmas classes.")

        # Combinar Associação e Agregação simultaneamente entre as mesmas duas classes
        if edge_type in ("Association", "Aggregation"):
            other_type = "Aggregation" if edge_type == "Association" else "Association"
            for e in model.edges:
                if e.type == other_type and {e.source_id, e.target_id} == {src.id, dst.id}:
                    return ValidationResult(False, "Não é permitido combinar Associação e Agregação entre as mesmas duas classes.")

    # 3. Diagrama de Objetos
    if dtype == "Object Diagram":
        if edge_type == "Collaboration":
            if src.type != "Object" or dst.type != "Object":
                return ValidationResult(False, "Colaboração só pode conectar dois Objetos.")
            for e in model.edges:
                if e.type == "Collaboration" and e.source_id == dst.id and e.target_id == src.id:
                    return ValidationResult(False, "Ciclo direto de colaboração entre Objetos não é permitido.")

    # 4. Diagrama de Casos de Uso
    if dtype == "Use Case Diagram":
        if edge_type in ("Include", "Extend"):
            if src.type != "Use Case" or dst.type != "Use Case":
                return ValidationResult(False, f"'{edge_type}' só pode conectar dois Casos de Uso.")
        elif edge_type == "Generalization":
            if src.type != dst.type:
                return ValidationResult(False, "Generalização exige dois Atores ou dois Casos de Uso.")
        elif edge_type == "Association":
            if not ({src.type, dst.type} == {"Actor", "Use Case"}):
                return ValidationResult(False, "Associação em Casos de Uso deve ligar um Ator a um Caso de Uso.")

    return ValidationResult(True)


# ==============================================================================
# 4. PARSER DE FORMATAÇÃO DE TEXTO UML (RF24: /itálico/ e _sublinhado_)
# ==============================================================================

TOKEN_PATTERN = re.compile(r"(/[^\n/]+/|_[^\n_]+_)")


def parse_formatted_line(line: str) -> list[tuple[str, bool, bool]]:
    """
    Divide uma linha de texto em segmentos (texto, italico, sublinhado)
    conforme RF24: /texto/ -> itálico, _texto_ -> sublinhado.
    """
    if not line:
        return [("", False, False)]
    segments = []
    pos = 0
    for m in TOKEN_PATTERN.finditer(line):
        if m.start() > pos:
            segments.append((line[pos:m.start()], False, False))
        tok = m.group(0)
        if tok.startswith("/") and tok.endswith("/"):
            inner = tok[1:-1]
            if inner.startswith("_") and inner.endswith("_") and len(inner) > 2:
                segments.append((inner[1:-1], True, True))
            else:
                segments.append((inner, True, False))
        elif tok.startswith("_") and tok.endswith("_"):
            inner = tok[1:-1]
            if inner.startswith("/") and inner.endswith("/") and len(inner) > 2:
                segments.append((inner[1:-1], True, True))
            else:
                segments.append((inner, False, True))
        pos = m.end()
    if pos < len(line):
        segments.append((line[pos:], False, False))
    return segments or [("", False, False)]


# ==============================================================================
# 5. LAYOUT, GEOMETRIA E RENDERIZADOR UNIFICADO (CANVAS + SVG + PNG)
# ==============================================================================

class RenderCommand:
    """Comando vetorial independente de dispositivo (Canvas Tkinter, SVG ou Raster)."""
    def __init__(self, kind: str, **kwargs):
        self.kind = kind
        self.data = kwargs


def sync_model_layout(model: DiagramModel):
    """Recalcula dimensões automáticas de nós compostos (Classes, Objetos, Pacotes, Sequência)."""
    # 1. Ajustar tamanho de nós de texto
    for n in model.nodes:
        if n.type == "Class":
            attrs = [a for a in n.properties.get("attributes", "").splitlines() if a.strip()]
            meths = [m for m in n.properties.get("methods", "").splitlines() if m.strip()]
            all_lines = [n.properties.get("name", "ClassName")] + attrs + meths
            max_len = max((len(x) for x in all_lines), default=10)
            n.width = max(140, snap_val(max_len * 8 + 28))
            h = 32 + max(20, len(attrs) * 18 + 8) + max(20, len(meths) * 18 + 8)
            n.height = max(90, snap_val(h))
        elif n.type == "Interface":
            meths = [m for m in n.properties.get("methods", "").splitlines() if m.strip()]
            all_lines = [n.properties.get("name", "InterfaceName"), "«interface»"] + meths
            max_len = max((len(x) for x in all_lines), default=12)
            n.width = max(140, snap_val(max_len * 8 + 28))
            h = 44 + max(24, len(meths) * 18 + 10)
            n.height = max(80, snap_val(h))
        elif n.type == "Note":
            lines = n.properties.get("text", "Note").splitlines() or ["Note"]
            max_len = max((len(x) for x in lines), default=8)
            n.width = max(120, snap_val(max_len * 7.5 + 28))
            n.height = max(60, snap_val(len(lines) * 18 + 24))
        elif n.type == "State":
            name = n.properties.get("name", "State")
            n.width = max(100, snap_val(len(name) * 8 + 30))
        elif n.type == "Use Case":
            name = n.properties.get("name", "Use Case")
            n.width = max(120, snap_val(len(name) * 8 + 40))
        elif n.type == "Implicit Parameter":
            name = n.properties.get("name", "object:Type")
            n.width = max(110, snap_val(len(name) * 8 + 28))
        elif n.type == "Package Description":
            lines = n.properties.get("contents", "").splitlines()
            name = n.properties.get("name", "package")
            max_len = max([len(name) + 4] + [len(x) for x in lines], default=12)
            n.width = max(170, snap_val(max_len * 8 + 24))
            n.height = max(100, snap_val(34 + max(3, len(lines)) * 18 + 16))

    # 2. Ajustar Objetos e seus Campos (RF25)
    for n in model.nodes:
        if n.type == "Object":
            fields = [f for f in model.nodes if f.type == "Field" and f.parent_id == n.id]
            name_len = len(n.properties.get("name", "object:Class"))
            max_field_len = max(
                (len(f"{f.properties.get('name', '')} = {f.properties.get('value', '')}") for f in fields),
                default=8,
            )
            n.width = max(140, snap_val(max(name_len, max_field_len) * 8 + 36))
            fy = n.y + 34
            for f in fields:
                f.x = n.x + 8
                f.y = fy
                f.width = n.width - 16
                f.height = 24
                fy += 26
            n.height = max(60, snap_val((fy - n.y) + 8))

    # 3. Ajustar CallNodes nas linhas de vida de Implicit Parameters (RF27-RF29)
    for n in model.nodes:
        if n.type == "CallNode":
            parent = model.get_node(n.parent_id)
            if parent and parent.type == "Implicit Parameter":
                depth = n.properties.get("nest_depth", 0)
                n.x = parent.x + parent.width / 2.0 - n.width / 2.0 + depth * 8
                min_y = parent.y + parent.height + 10
                if n.y < min_y:
                    n.y = min_y

    # 4. Ajustar Pacotes para englobar seus filhos (RF15)
    packages = [n for n in model.nodes if n.type == "Package"]
    for pkg in reversed(packages):
        kids = model.children_of(pkg.id)
        if kids:
            min_x = min(k.x for k in kids) - 16
            min_y = min(k.y for k in kids) - 34
            max_x = max(k.x + k.width for k in kids) + 16
            max_y = max(k.y + k.height for k in kids) + 16
            if min_x < pkg.x:
                pkg.width += (pkg.x - min_x)
                pkg.x = min_x
            if min_y < pkg.y:
                pkg.height += (pkg.y - min_y)
                pkg.y = min_y
            if max_x > pkg.x + pkg.width:
                pkg.width = max_x - pkg.x
            if max_y > pkg.y + pkg.height:
                pkg.height = max_y - pkg.y


def rect_border_point(node: DiagramNode, target_x: float, target_y: float) -> tuple[float, float]:
    """Calcula a interseção da linha entre o centro do nó e (target_x, target_y) com a borda."""
    cx, cy = node.center()
    dx = target_x - cx
    dy = target_y - cy
    if abs(dx) < 1e-6 and abs(dy) < 1e-6:
        return (cx, node.y)

    if node.type == "Use Case":
        rx = node.width / 2.0
        ry = node.height / 2.0
        angle = math.atan2(dy, dx)
        return (cx + rx * math.cos(angle), cy + ry * math.sin(angle))

    if node.type in ("Initial State", "Final State"):
        r = node.width / 2.0
        angle = math.atan2(dy, dx)
        return (cx + r * math.cos(angle), cy + r * math.sin(angle))

    hw = node.width / 2.0
    hh = node.height / 2.0
    tx = hw / abs(dx) if abs(dx) > 1e-6 else float("inf")
    ty = hh / abs(dy) if abs(dy) > 1e-6 else float("inf")
    t = min(tx, ty)
    return (cx + dx * t, cy + dy * t)


def compute_edge_points(model: DiagramModel, edge: DiagramEdge) -> list[tuple[float, float]]:
    """Calcula a polilinha automática da aresta (RF17, RF19, RF26, RF27-RF30)."""
    src = model.get_node(edge.source_id)
    dst = model.get_node(edge.target_id)
    if not src or not dst:
        return []

    # Caso 1: Referência em Diagrama de Objetos (parte do lado direito da área Value do Campo)
    if edge.type == "Reference" and src.type == "Field":
        sx = src.x + src.width - 14
        sy = src.y + src.height / 2.0
        tx, ty = rect_border_point(dst, sx + 40, sy)
        mid_x = max(sx + 25, (sx + tx) / 2.0)
        return [(sx, sy), (mid_x, sy), (mid_x, ty), (tx, ty)]

    # Caso 2: Chamadas e Retornos em Diagrama de Sequência (RF27-RF30)
    if edge.type in ("Call", "Return"):
        if edge.properties.get("is_create") and dst.type == "Implicit Parameter":
            sy = dst.y + dst.height / 2.0
            sx = src.x + src.width if src.center()[0] < dst.center()[0] else src.x
            tx = dst.x if src.center()[0] < dst.center()[0] else dst.x + dst.width
            return [(sx, sy), (tx, sy)]

        if src.id == dst.id or (src.parent_id and src.parent_id == dst.parent_id):
            # Autochamada em U (RF28)
            sx = src.x + src.width
            sy = src.y + 8
            tx = dst.x + dst.width
            ty = max(sy + 22, dst.y + 6)
            return [(sx, sy), (sx + 28, sy), (sx + 28, ty), (tx, ty)]

        if edge.type == "Call":
            sy = max(src.y + 10, min(dst.y + 6, src.y + src.height - 6))
        else:
            sy = src.y + src.height - 6

        if src.center()[0] <= dst.center()[0]:
            sx = src.x + src.width
            tx = dst.x
        else:
            sx = src.x
            tx = dst.x + dst.width
        return [(sx, sy), (tx, sy)]

    # Caso 3: Autoaresta (RF19)
    if src.id == dst.id:
        same_self = [
            e for e in model.edges
            if e.source_id == src.id and e.target_id == dst.id and e.type == edge.type
        ]
        idx = same_self.index(edge) if edge in same_self else 0
        if idx == 0:
            p1 = (src.x + src.width * 0.65, src.y)
            p2 = (src.x + src.width * 0.65, src.y - 28)
            p3 = (src.x + src.width + 28, src.y - 28)
            p4 = (src.x + src.width + 28, src.y + src.height * 0.4)
            p5 = (src.x + src.width, src.y + src.height * 0.4)
            return [p1, p2, p3, p4, p5]
        else:
            p1 = (src.x + src.width * 0.35, src.y + src.height)
            p2 = (src.x + src.width * 0.35, src.y + src.height + 28)
            p3 = (src.x - 28, src.y + src.height + 28)
            p4 = (src.x - 28, src.y + src.height * 0.6)
            p5 = (src.x, src.y + src.height * 0.6)
            return [p1, p2, p3, p4, p5]

    # Caso 4: Aresta entre dois nós distintos (com deslocamento paralelo se houver múltiplas)
    pair_edges = [
        e for e in model.edges
        if {e.source_id, e.target_id} == {src.id, dst.id}
    ]
    pair_idx = pair_edges.index(edge) if edge in pair_edges else 0
    offset = 0
    if len(pair_edges) > 1:
        offset = (-12 if pair_idx % 2 == 0 else 12) * ((pair_idx // 2) + 1)

    scx, scy = src.center()
    dcx, dcy = dst.center()
    dx = dcx - scx
    dy = dcy - scy
    length = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / length, dx / length  # Vetor normal perpendicular

    p_start = rect_border_point(src, dcx + nx * offset, dcy + ny * offset)
    p_end = rect_border_point(dst, scx + nx * offset, scy + ny * offset)

    if abs(offset) > 0:
        mid = ((p_start[0] + p_end[0]) / 2.0 + nx * offset, (p_start[1] + p_end[1]) / 2.0 + ny * offset)
        return [p_start, mid, p_end]
    return [p_start, p_end]


def build_scene_commands(
    model: DiagramModel,
    theme: dict,
    selected_ids: set[str] = None,
    include_grid: bool = True,
    highlight_pkg_id: str = None,
    font_family: str = "Segoe UI",
    font_size: int = 10,
) -> list[RenderCommand]:
    """Constrói toda a cena vetorial do diagrama para renderização em Tela, SVG ou PNG."""
    selected_ids = selected_ids or set()
    sync_model_layout(model)
    cmds: list[RenderCommand] = []

    # Fundo e grade (RF05 omite grade ao copiar para área de transferência)
    cmds.append(RenderCommand("rect", x1=0, y1=0, x2=model.width, y2=model.height, fill=theme["canvas_bg"], outline=""))
    if include_grid:
        for gx in range(GRID_SIZE, model.width, GRID_SIZE * 2):
            cmds.append(RenderCommand("line", points=[(gx, 0), (gx, model.height)], fill=theme["grid"], width=1, dash=()))
        for gy in range(GRID_SIZE, model.height, GRID_SIZE * 2):
            cmds.append(RenderCommand("line", points=[(0, gy), (model.width, gy)], fill=theme["grid"], width=1, dash=()))

    # Ordena nós para desenhar Pacotes primeiro (atrás dos filhos), depois demais nós
    ordered_nodes = sorted(
        model.nodes,
        key=lambda n: 0 if n.type == "Package" else (2 if n.type in ("Field", "CallNode") else 1),
    )

    # Linhas de vida no Diagrama de Sequência (abaixo das caixas de ativação)
    if model.diagram_type == "Sequence Diagram":
        max_lifeline_y = model.height - 40
        for n in model.nodes:
            if n.type == "Implicit Parameter":
                lx = n.x + n.width / 2.0
                ly1 = n.y + n.height
                cmds.append(RenderCommand(
                    "line",
                    points=[(lx, ly1), (lx, max_lifeline_y)],
                    fill=theme["stroke"],
                    width=1.2,
                    dash=(5, 4),
                ))

    # Desenha Nós
    for n in ordered_nodes:
        is_sel = n.id in selected_ids
        stroke = theme["select"] if is_sel else theme["stroke"]
        lw = 2.2 if is_sel else 1.4

        if n.type == "PointNode":
            cmds.append(RenderCommand("oval", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=stroke, outline=stroke, width=1))
            continue

        if n.type == "Class":
            attrs = [a for a in n.properties.get("attributes", "").splitlines() if a.strip()]
            meths = [m for m in n.properties.get("methods", "").splitlines() if m.strip()]
            h_head = 32
            h_attr = max(20, len(attrs) * 18 + 8)
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["node_fill"], outline=stroke, width=lw))
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + h_head, fill=theme["node_header"], outline=stroke, width=lw))
            cmds.append(RenderCommand("line", points=[(n.x, n.y + h_head + h_attr), (n.x + n.width, n.y + h_head + h_attr)], fill=stroke, width=1.2, dash=()))
            cmds.append(RenderCommand("rich_text", x=n.x + n.width / 2.0, y=n.y + 16, text=n.properties.get("name", "ClassName"), anchor="center", bold=True, fill=theme["fg"], font_family=font_family, font_size=font_size))
            for idx, line in enumerate(attrs):
                cmds.append(RenderCommand("rich_text", x=n.x + 8, y=n.y + h_head + 12 + idx * 18, text=line, anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))
            for idx, line in enumerate(meths):
                cmds.append(RenderCommand("rich_text", x=n.x + 8, y=n.y + h_head + h_attr + 12 + idx * 18, text=line, anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "Interface":
            meths = [m for m in n.properties.get("methods", "").splitlines() if m.strip()]
            h_head = 44
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["node_fill"], outline=stroke, width=lw))
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + h_head, fill=theme["node_header"], outline=stroke, width=lw))
            cmds.append(RenderCommand("rich_text", x=n.x + n.width / 2.0, y=n.y + 13, text="«interface»", anchor="center", bold=False, fill=theme["muted_fg"], font_family=font_family, font_size=max(8, font_size - 1)))
            cmds.append(RenderCommand("rich_text", x=n.x + n.width / 2.0, y=n.y + 30, text=n.properties.get("name", "InterfaceName"), anchor="center", bold=True, fill=theme["fg"], font_family=font_family, font_size=font_size))
            for idx, line in enumerate(meths):
                cmds.append(RenderCommand("rich_text", x=n.x + 8, y=n.y + h_head + 12 + idx * 18, text=line, anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type in ("Package", "Package Description"):
            pkg_stroke = theme["package_highlight"] if n.id == highlight_pkg_id else stroke
            pkg_lw = 2.8 if n.id == highlight_pkg_id else lw
            tab_w = min(n.width * 0.65, max(75, len(n.properties.get("name", "package")) * 8 + 20))
            tab_h = 24
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + tab_w, y2=n.y + tab_h, fill=theme["node_header"], outline=pkg_stroke, width=pkg_lw))
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y + tab_h, x2=n.x + n.width, y2=n.y + n.height, fill=theme["package_fill"], outline=pkg_stroke, width=pkg_lw))
            cmds.append(RenderCommand("rich_text", x=n.x + 8, y=n.y + 12, text=n.properties.get("name", "package"), anchor="w", bold=True, fill=theme["fg"], font_family=font_family, font_size=font_size))
            if n.type == "Package Description":
                for idx, line in enumerate(n.properties.get("contents", "").splitlines()):
                    cmds.append(RenderCommand("rich_text", x=n.x + 10, y=n.y + tab_h + 14 + idx * 18, text=line, anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "Note":
            fold = 14
            x1, y1, x2, y2 = n.bounds()
            poly = [(x1, y1), (x2 - fold, y1), (x2, y1 + fold), (x2, y2), (x1, y2)]
            cmds.append(RenderCommand("polygon", points=poly, fill=theme["note_fill"], outline=stroke, width=lw))
            cmds.append(RenderCommand("polygon", points=[(x2 - fold, y1), (x2 - fold, y1 + fold), (x2, y1 + fold)], fill=theme["node_header"], outline=stroke, width=1.1))
            for idx, line in enumerate(n.properties.get("text", "Note").splitlines()):
                cmds.append(RenderCommand("rich_text", x=x1 + 8, y=y1 + 16 + idx * 18, text=line, anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "Implicit Parameter":
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["node_header"], outline=stroke, width=lw))
            name_txt = n.properties.get("name", "object:Type")
            cmds.append(RenderCommand("rich_text", x=n.x + n.width / 2.0, y=n.y + n.height / 2.0, text=f"_{name_txt}_", anchor="center", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "CallNode":
            open_bottom = bool(n.properties.get("open_bottom", False))
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["node_fill"], outline="", width=0))
            cmds.append(RenderCommand("line", points=[(n.x, n.y), (n.x + n.width, n.y)], fill=stroke, width=lw, dash=()))
            cmds.append(RenderCommand("line", points=[(n.x, n.y), (n.x, n.y + n.height)], fill=stroke, width=lw, dash=()))
            cmds.append(RenderCommand("line", points=[(n.x + n.width, n.y), (n.x + n.width, n.y + n.height)], fill=stroke, width=lw, dash=()))
            if not open_bottom:
                cmds.append(RenderCommand("line", points=[(n.x, n.y + n.height), (n.x + n.width, n.y + n.height)], fill=stroke, width=lw, dash=()))
            else:
                # RF31: Open Bottom indica fluxo de chamada incompleto
                cmds.append(RenderCommand("line", points=[(n.x, n.y + n.height), (n.x, n.y + n.height + 12)], fill=stroke, width=lw, dash=(3, 3)))
                cmds.append(RenderCommand("line", points=[(n.x + n.width, n.y + n.height), (n.x + n.width, n.y + n.height + 12)], fill=stroke, width=lw, dash=(3, 3)))

        elif n.type == "State":
            cmds.append(RenderCommand("round_rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, radius=16, fill=theme["node_fill"], outline=stroke, width=lw))
            cmds.append(RenderCommand("rich_text", x=n.x + n.width / 2.0, y=n.y + n.height / 2.0, text=n.properties.get("name", "State"), anchor="center", bold=True, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "Initial State":
            cmds.append(RenderCommand("oval", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["stroke"], outline=stroke, width=lw))

        elif n.type == "Final State":
            cmds.append(RenderCommand("oval", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["canvas_bg"], outline=stroke, width=lw))
            cmds.append(RenderCommand("oval", x1=n.x + 5, y1=n.y + 5, x2=n.x + n.width - 5, y2=n.y + n.height - 5, fill=theme["stroke"], outline=stroke, width=1))

        elif n.type == "Object":
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["node_fill"], outline=stroke, width=lw))
            cmds.append(RenderCommand("rect", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + 30, fill=theme["node_header"], outline=stroke, width=lw))
            obj_name = n.properties.get("name", "object:Class")
            cmds.append(RenderCommand("rich_text", x=n.x + n.width / 2.0, y=n.y + 15, text=f"_{obj_name}_", anchor="center", bold=True, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "Field":
            fname = n.properties.get("name", "field")
            fval = n.properties.get("value", "value")
            has_ref = any(e.type == "Reference" and e.source_id == n.id for e in model.edges)
            if is_sel:
                cmds.append(RenderCommand("rect", x1=n.x - 2, y1=n.y - 2, x2=n.x + n.width + 2, y2=n.y + n.height + 2, fill="", outline=theme["select"], width=1.6))
            cmds.append(RenderCommand("rich_text", x=n.x + 4, y=n.y + n.height / 2.0, text=f"{fname} = ", anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))
            val_x = n.x + n.width * 0.52
            cmds.append(RenderCommand("rect", x1=val_x, y1=n.y + 2, x2=n.x + n.width - 2, y2=n.y + n.height - 2, fill=theme["canvas_bg"], outline=theme["muted_fg"], width=1))
            if has_ref:
                cx_dot = n.x + n.width - 14
                cy_dot = n.y + n.height / 2.0
                cmds.append(RenderCommand("oval", x1=cx_dot - 3.5, y1=cy_dot - 3.5, x2=cx_dot + 3.5, y2=cy_dot + 3.5, fill=theme["stroke"], outline=theme["stroke"], width=1))
            else:
                cmds.append(RenderCommand("rich_text", x=val_x + 5, y=n.y + n.height / 2.0, text=fval, anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "Actor":
            cx = n.x + n.width / 2.0
            cmds.append(RenderCommand("oval", x1=cx - 10, y1=n.y + 2, x2=cx + 10, y2=n.y + 22, fill=theme["node_fill"], outline=stroke, width=lw))
            cmds.append(RenderCommand("line", points=[(cx, n.y + 22), (cx, n.y + 48)], fill=stroke, width=lw, dash=()))
            cmds.append(RenderCommand("line", points=[(cx - 16, n.y + 30), (cx + 16, n.y + 30)], fill=stroke, width=lw, dash=()))
            cmds.append(RenderCommand("line", points=[(cx, n.y + 48), (cx - 14, n.y + 66)], fill=stroke, width=lw, dash=()))
            cmds.append(RenderCommand("line", points=[(cx, n.y + 48), (cx + 14, n.y + 66)], fill=stroke, width=lw, dash=()))
            cmds.append(RenderCommand("rich_text", x=cx, y=n.y + 77, text=n.properties.get("name", "Actor"), anchor="center", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

        elif n.type == "Use Case":
            cmds.append(RenderCommand("oval", x1=n.x, y1=n.y, x2=n.x + n.width, y2=n.y + n.height, fill=theme["node_fill"], outline=stroke, width=lw))
            cmds.append(RenderCommand("rich_text", x=n.x + n.width / 2.0, y=n.y + n.height / 2.0, text=n.properties.get("name", "Use Case"), anchor="center", bold=False, fill=theme["fg"], font_family=font_family, font_size=font_size))

    # Desenha Arestas
    for e in model.edges:
        pts = compute_edge_points(model, e)
        if len(pts) < 2:
            continue
        is_sel = e.id in selected_ids
        stroke = theme["select"] if is_sel else theme["stroke"]
        lw = 2.4 if is_sel else 1.5

        eff_type = e.type
        if e.type in ("Aggregation", "Composition"):
            eff_type = e.properties.get("aggregation_type", e.type)
        elif e.type in ("Include", "Extend", "Association") and model.diagram_type == "Use Case Diagram":
            dep = e.properties.get("dependency_type")
            if dep in ("Include", "Extend"):
                eff_type = dep
            elif dep == "None":
                eff_type = "Association"

        dashed = eff_type in ("Dependency", "Realization", "Note Connector", "Return", "Include", "Extend")
        dash = (6, 4) if dashed else ()
        cmds.append(RenderCommand("line", points=pts, fill=stroke, width=lw, dash=dash))

        # Pontas (setas, triângulos, losangos)
        p_prev, p_last = pts[-2], pts[-1]
        p_first, p_second = pts[0], pts[1]
        directionality = e.properties.get("directionality", "Unidirectional")

        if eff_type in ("Inheritance", "Realization", "Generalization"):
            _append_arrowhead(cmds, p_prev, p_last, stroke, theme["canvas_bg"], hollow_triangle=True)
            if directionality == "Bidirectional":
                _append_arrowhead(cmds, p_second, p_first, stroke, theme["canvas_bg"], hollow_triangle=True)
        elif eff_type in ("Dependency", "Call", "Return", "Transition", "Reference", "Include", "Extend"):
            _append_arrowhead(cmds, p_prev, p_last, stroke, stroke, open_vee=True)
            if directionality == "Bidirectional":
                _append_arrowhead(cmds, p_second, p_first, stroke, stroke, open_vee=True)
        elif eff_type in ("Aggregation", "Composition"):
            fill_d = theme["canvas_bg"] if eff_type == "Aggregation" else stroke
            _append_diamond(cmds, p_second, p_first, stroke, fill_d)
            if directionality == "Unidirectional":
                _append_arrowhead(cmds, p_prev, p_last, stroke, stroke, open_vee=True)
            elif directionality == "Bidirectional":
                _append_arrowhead(cmds, p_prev, p_last, stroke, stroke, open_vee=True)
        elif eff_type == "Association":
            if directionality == "Unidirectional":
                _append_arrowhead(cmds, p_prev, p_last, stroke, stroke, open_vee=True)
            elif directionality == "Bidirectional":
                _append_arrowhead(cmds, p_prev, p_last, stroke, stroke, open_vee=True)
                _append_arrowhead(cmds, p_second, p_first, stroke, stroke, open_vee=True)

        # Rótulos da aresta (Start, Middle, End)
        mid_pt = pts[len(pts) // 2] if len(pts) % 2 == 1 else (
            (pts[len(pts) // 2 - 1][0] + pts[len(pts) // 2][0]) / 2.0,
            (pts[len(pts) // 2 - 1][1] + pts[len(pts) // 2][1]) / 2.0,
        )
        mid_label = e.properties.get("middle_label", "")
        if eff_type == "Include":
            mid_label = "«include»"
        elif eff_type == "Extend":
            mid_label = "«extend»"

        if mid_label:
            cmds.append(RenderCommand("rich_text", x=mid_pt[0], y=mid_pt[1] - 11, text=mid_label, anchor="center", bold=False, fill=theme["fg"], font_family=font_family, font_size=max(8, font_size - 1)))
        start_lbl = e.properties.get("start_label", "")
        if start_lbl:
            cmds.append(RenderCommand("rich_text", x=p_first[0] + 10, y=p_first[1] - 10, text=start_lbl, anchor="w", bold=False, fill=theme["fg"], font_family=font_family, font_size=max(8, font_size - 1)))
        end_lbl = e.properties.get("end_label", "")
        if end_lbl:
            cmds.append(RenderCommand("rich_text", x=p_last[0] - 10, y=p_last[1] - 10, text=end_lbl, anchor="e", bold=False, fill=theme["fg"], font_family=font_family, font_size=max(8, font_size - 1)))

    return cmds


def _append_arrowhead(cmds, p_from, p_to, stroke, fill, hollow_triangle=False, open_vee=False):
    dx = p_to[0] - p_from[0]
    dy = p_to[1] - p_from[1]
    ang = math.atan2(dy, dx)
    size = 11
    a1 = ang + math.radians(152)
    a2 = ang - math.radians(152)
    pt1 = (p_to[0] + size * math.cos(a1), p_to[1] + size * math.sin(a1))
    pt2 = (p_to[0] + size * math.cos(a2), p_to[1] + size * math.sin(a2))
    if hollow_triangle:
        cmds.append(RenderCommand("polygon", points=[p_to, pt1, pt2], fill=fill, outline=stroke, width=1.5))
    elif open_vee:
        cmds.append(RenderCommand("line", points=[pt1, p_to, pt2], fill=stroke, width=1.6, dash=()))


def _append_diamond(cmds, p_from, p_tip, stroke, fill):
    dx = p_tip[0] - p_from[0]
    dy = p_tip[1] - p_from[1]
    ang = math.atan2(dy, dx)
    size = 9
    a1 = ang + math.radians(145)
    a2 = ang - math.radians(145)
    pt1 = (p_tip[0] + size * math.cos(a1), p_tip[1] + size * math.sin(a1))
    pt2 = (p_tip[0] + size * math.cos(a2), p_tip[1] + size * math.sin(a2))
    pt_back = (p_tip[0] + size * 1.65 * math.cos(ang + math.pi), p_tip[1] + size * 1.65 * math.sin(ang + math.pi))
    cmds.append(RenderCommand("polygon", points=[p_tip, pt1, pt_back, pt2], fill=fill, outline=stroke, width=1.4))


def compute_content_bbox(model: DiagramModel, pad: int = 20) -> tuple[int, int, int, int]:
    """Calcula a caixa envolvente ajustada ao conteúdo do diagrama (RF05)."""
    sync_model_layout(model)
    if not model.nodes:
        return (0, 0, min(400, model.width), min(300, model.height))
    xs, ys = [], []
    for n in model.nodes:
        xs.extend([n.x, n.x + n.width])
        ys.extend([n.y, n.y + n.height])
    for e in model.edges:
        for px, py in compute_edge_points(model, e):
            xs.append(px)
            ys.append(py)
    x1 = max(0, int(min(xs)) - pad)
    y1 = max(0, int(min(ys)) - pad)
    x2 = min(model.width, int(max(xs)) + pad)
    y2 = min(model.height, int(max(ys)) + pad)
    if x2 <= x1 + 10:
        x2 = min(model.width, x1 + 200)
    if y2 <= y1 + 10:
        y2 = min(model.height, y1 + 150)
    return (x1, y1, x2, y2)


# ==============================================================================
# 6. EXPORTAÇÃO VETORIAL (SVG), RASTER (PNG/JPEG/PPM) E CLIPBOARD (RF04, RF05)
# ==============================================================================

def _escape_xml(s: str) -> str:
    return (
        s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def export_diagram_svg(model: DiagramModel, filepath: str, theme: dict, font_family: str, font_size: int, crop: bool = False):
    """Exporta o diagrama em formato vetorial SVG puro (RF04)."""
    cmds = build_scene_commands(model, theme, include_grid=not crop, font_family=font_family, font_size=font_size)
    if crop:
        bx1, by1, bx2, by2 = compute_content_bbox(model)
        w, h = bx2 - bx1, by2 - by1
        vb = f"{bx1} {by1} {w} {h}"
    else:
        w, h = model.width, model.height
        vb = f"0 0 {w} {h}"

    lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="{vb}">',
    ]
    for c in cmds:
        d = c.data
        if c.kind == "rect":
            fill = d["fill"] if d["fill"] else "none"
            stroke = d["outline"] if d["outline"] else "none"
            sw = d.get("width", 1)
            lines.append(f'  <rect x="{d["x1"]}" y="{d["y1"]}" width="{d["x2"]-d["x1"]}" height="{d["y2"]-d["y1"]}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>')
        elif c.kind == "round_rect":
            fill = d["fill"] if d["fill"] else "none"
            stroke = d["outline"] if d["outline"] else "none"
            r = d.get("radius", 12)
            lines.append(f'  <rect x="{d["x1"]}" y="{d["y1"]}" rx="{r}" ry="{r}" width="{d["x2"]-d["x1"]}" height="{d["y2"]-d["y1"]}" fill="{fill}" stroke="{stroke}" stroke-width="{d.get("width",1)}"/>')
        elif c.kind == "oval":
            cx = (d["x1"] + d["x2"]) / 2.0
            cy = (d["y1"] + d["y2"]) / 2.0
            rx = (d["x2"] - d["x1"]) / 2.0
            ry = (d["y2"] - d["y1"]) / 2.0
            fill = d["fill"] if d["fill"] else "none"
            stroke = d["outline"] if d["outline"] else "none"
            lines.append(f'  <ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{fill}" stroke="{stroke}" stroke-width="{d.get("width",1)}"/>')
        elif c.kind == "polygon":
            pts_str = " ".join(f"{x},{y}" for x, y in d["points"])
            fill = d["fill"] if d["fill"] else "none"
            stroke = d["outline"] if d["outline"] else "none"
            lines.append(f'  <polygon points="{pts_str}" fill="{fill}" stroke="{stroke}" stroke-width="{d.get("width",1)}"/>')
        elif c.kind == "line":
            pts_str = " ".join(f"{x},{y}" for x, y in d["points"])
            dash_attr = f' stroke-dasharray="{",".join(map(str, d["dash"]))}"' if d.get("dash") else ""
            lines.append(f'  <polyline points="{pts_str}" fill="none" stroke="{d["fill"]}" stroke-width="{d.get("width",1)}"{dash_attr}/>')
        elif c.kind == "rich_text":
            anchor_map = {"w": "start", "center": "middle", "e": "end"}
            ta = anchor_map.get(d["anchor"], "start")
            fw = "bold" if d.get("bold") else "normal"
            segs = parse_formatted_line(d["text"])
            tspans = []
            for seg_txt, is_it, is_ul in segs:
                style_parts = []
                if is_it:
                    style_parts.append("font-style:italic")
                if is_ul:
                    style_parts.append("text-decoration:underline")
                st = f' style="{";".join(style_parts)}"' if style_parts else ""
                tspans.append(f"<tspan{st}>{_escape_xml(seg_txt)}</tspan>")
            lines.append(
                f'  <text x="{d["x"]}" y="{d["y"]+4}" fill="{d["fill"]}" font-family="{_escape_xml(d["font_family"])}" '
                f'font-size="{d["font_size"]}pt" font-weight="{fw}" text-anchor="{ta}">{"".join(tspans)}</text>'
            )
    lines.append("</svg>")
    with open(filepath, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def _hex_to_rgb(hex_color: str) -> tuple[int, int, int]:
    if not hex_color or not hex_color.startswith("#"):
        return (255, 255, 255)
    h = hex_color.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def _write_pure_png(filepath: str, width: int, height: int, rgb_rows: list[bytes]):
    """Codificador PNG puro em Python (sem dependências externas)."""
    raw = b"".join(b"\x00" + row for row in rgb_rows)
    compressed = zlib.compress(raw, level=6)

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack("!I", len(data)) + tag + data + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack("!IIBBBBB", width, height, 8, 2, 0, 0, 0)
    png_bytes = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")
    with open(filepath, "wb") as f:
        f.write(png_bytes)


def render_diagram_raster(model: DiagramModel, filepath: str, theme: dict, font_family: str, font_size: int, crop: bool = False):
    """Renderiza imagem raster (PNG, JPEG, PPM) com Pillow ou motor puro Python (RF04, RF05)."""
    cmds = build_scene_commands(model, theme, include_grid=not crop, font_family=font_family, font_size=font_size)
    if crop:
        bx1, by1, bx2, by2 = compute_content_bbox(model)
    else:
        bx1, by1, bx2, by2 = 0, 0, model.width, model.height

    w = max(1, bx2 - bx1)
    h = max(1, by2 - by1)
    ext = os.path.splitext(filepath)[1].lower()

    if HAS_PIL:
        bg_rgb = _hex_to_rgb(theme["canvas_bg"])
        img = Image.new("RGB", (w, h), bg_rgb)
        draw = ImageDraw.Draw(img)
        try:
            pil_font = ImageFont.truetype("arial.ttf", font_size + 2)
        except Exception:
            pil_font = ImageFont.load_default()

        for c in cmds:
            d = c.data
            if c.kind in ("rect", "round_rect"):
                x1, y1, x2, y2 = d["x1"] - bx1, d["y1"] - by1, d["x2"] - bx1, d["y2"] - by1
                fill = _hex_to_rgb(d["fill"]) if d.get("fill") else None
                out = _hex_to_rgb(d["outline"]) if d.get("outline") else None
                lw = max(1, int(round(d.get("width", 1))))
                if c.kind == "round_rect":
                    draw.rounded_rectangle([x1, y1, x2, y2], radius=d.get("radius", 12), fill=fill, outline=out, width=lw)
                else:
                    draw.rectangle([x1, y1, x2, y2], fill=fill, outline=out, width=lw)
            elif c.kind == "oval":
                x1, y1, x2, y2 = d["x1"] - bx1, d["y1"] - by1, d["x2"] - bx1, d["y2"] - by1
                fill = _hex_to_rgb(d["fill"]) if d.get("fill") else None
                out = _hex_to_rgb(d["outline"]) if d.get("outline") else None
                draw.ellipse([x1, y1, x2, y2], fill=fill, outline=out, width=max(1, int(round(d.get("width", 1)))))
            elif c.kind == "polygon":
                pts = [(x - bx1, y - by1) for x, y in d["points"]]
                fill = _hex_to_rgb(d["fill"]) if d.get("fill") else None
                out = _hex_to_rgb(d["outline"]) if d.get("outline") else None
                draw.polygon(pts, fill=fill, outline=out)
            elif c.kind == "line":
                pts = [(x - bx1, y - by1) for x, y in d["points"]]
                col = _hex_to_rgb(d["fill"])
                lw = max(1, int(round(d.get("width", 1))))
                draw.line(pts, fill=col, width=lw)
            elif c.kind == "rich_text":
                clean_txt = "".join(seg[0] for seg in parse_formatted_line(d["text"]))
                tx, ty = d["x"] - bx1, d["y"] - by1 - 6
                if d["anchor"] == "center":
                    tx -= len(clean_txt) * 3.2
                elif d["anchor"] == "e":
                    tx -= len(clean_txt) * 6.5
                draw.text((tx, ty), clean_txt, fill=_hex_to_rgb(d["fill"]), font=pil_font)

        if ext in (".jpg", ".jpeg"):
            img.save(filepath, format="JPEG", quality=95)
        else:
            img.save(filepath, format="PNG")
        return

    # Fallback 100% puro Python sem Pillow (renderiza formas e gera PNG/PPM válido)
    bg_r, bg_g, bg_b = _hex_to_rgb(theme["canvas_bg"])
    buf = bytearray([bg_r, bg_g, bg_b] * (w * h))

    def set_px(px: int, py: int, rgb: tuple[int, int, int]):
        if 0 <= px < w and 0 <= py < h:
            idx = (py * w + px) * 3
            buf[idx], buf[idx + 1], buf[idx + 2] = rgb

    def draw_seg(x0, y0, x1, y1, rgb):
        x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
        dx, dy = abs(x1 - x0), abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx - dy
        while True:
            set_px(x0, y0, rgb)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 > -dy:
                err -= dy
                x0 += sx
            if e2 < dx:
                err += dx
                y0 += sy

    for c in cmds:
        d = c.data
        if c.kind in ("rect", "round_rect"):
            x1 = max(0, int(round(d["x1"] - bx1)))
            y1 = max(0, int(round(d["y1"] - by1)))
            x2 = min(w - 1, int(round(d["x2"] - bx1)))
            y2 = min(h - 1, int(round(d["y2"] - by1)))
            if d.get("fill"):
                frgb = _hex_to_rgb(d["fill"])
                row_bytes = bytes(frgb) * max(0, x2 - x1 + 1)
                for py in range(y1, y2 + 1):
                    start = (py * w + x1) * 3
                    buf[start:start + len(row_bytes)] = row_bytes
            if d.get("outline"):
                orgb = _hex_to_rgb(d["outline"])
                draw_seg(x1, y1, x2, y1, orgb)
                draw_seg(x2, y1, x2, y2, orgb)
                draw_seg(x2, y2, x1, y2, orgb)
                draw_seg(x1, y2, x1, y1, orgb)
        elif c.kind == "oval":
            cx = (d["x1"] + d["x2"]) / 2.0 - bx1
            cy = (d["y1"] + d["y2"]) / 2.0 - by1
            rx = max(1.0, (d["x2"] - d["x1"]) / 2.0)
            ry = max(1.0, (d["y2"] - d["y1"]) / 2.0)
            if d.get("fill"):
                frgb = _hex_to_rgb(d["fill"])
                for py in range(max(0, int(cy - ry)), min(h, int(cy + ry) + 1)):
                    for px in range(max(0, int(cx - rx)), min(w, int(cx + rx) + 1)):
                        if ((px - cx) / rx) ** 2 + ((py - cy) / ry) ** 2 <= 1.0:
                            set_px(px, py, frgb)
            if d.get("outline"):
                orgb = _hex_to_rgb(d["outline"])
                steps = 48
                for i in range(steps):
                    a1 = 2 * math.pi * i / steps
                    a2 = 2 * math.pi * (i + 1) / steps
                    draw_seg(cx + rx * math.cos(a1), cy + ry * math.sin(a1), cx + rx * math.cos(a2), cy + ry * math.sin(a2), orgb)
        elif c.kind == "line":
            col = _hex_to_rgb(d["fill"])
            pts = d["points"]
            for i in range(len(pts) - 1):
                draw_seg(pts[i][0] - bx1, pts[i][1] - by1, pts[i + 1][0] - bx1, pts[i + 1][1] - by1, col)
        elif c.kind == "polygon":
            pts = d["points"]
            if d.get("outline"):
                col = _hex_to_rgb(d["outline"])
                for i in range(len(pts)):
                    p1, p2 = pts[i], pts[(i + 1) % len(pts)]
                    draw_seg(p1[0] - bx1, p1[1] - by1, p2[0] - bx1, p2[1] - by1, col)

    rows = [bytes(buf[y * w * 3:(y + 1) * w * 3]) for y in range(h)]
    if ext == ".ppm":
        with open(filepath, "wb") as f:
            f.write(f"P6\n{w} {h}\n255\n".encode("ascii"))
            for r in rows:
                f.write(r)
    else:
        _write_pure_png(filepath, w, h, rows)


def copy_image_to_system_clipboard(root: tk.Tk, png_path: str) -> bool:
    """Copia o arquivo PNG gerado para a área de transferência do sistema operacional (RF05)."""
    sys_name = platform.system()
    try:
        if sys_name == "Windows":
            ps_cmd = (
                "Add-Type -AssemblyName System.Windows.Forms,System.Drawing; "
                f"$img = [System.Drawing.Image]::FromFile('{png_path}'); "
                "[System.Windows.Forms.Clipboard]::SetImage($img); "
                "$img.Dispose()"
            )
            subprocess.run(["powershell", "-NoProfile", "-Command", ps_cmd], check=True, timeout=5)
            return True
        elif sys_name == "Darwin":
            osa = f'set the clipboard to (read (POSIX file "{png_path}") as «class PNGf»)'
            subprocess.run(["osascript", "-e", osa], check=True, timeout=5)
            return True
        else:
            for cmd in (
                ["xclip", "-selection", "clipboard", "-t", "image/png", "-i", png_path],
                ["wl-copy", "--type", "image/png"],
            ):
                try:
                    if cmd[0] == "wl-copy":
                        with open(png_path, "rb") as f:
                            subprocess.run(cmd, stdin=f, check=True, timeout=4)
                    else:
                        subprocess.run(cmd, check=True, timeout=4)
                    return True
                except Exception:
                    continue
    except Exception:
        pass
    # Fallback via Tkinter clipboard
    try:
        root.clipboard_clear()
        root.clipboard_append(png_path)
        return True
    except Exception:
        return False


# ==============================================================================
# 7. DIÁLOGOS DE PROPRIEDADES E CONFIGURAÇÃO (RF20, RF22, RF23, RF38, RF39, RF42)
# ==============================================================================

class PropertiesDialog(tk.Toplevel):
    """
    Painel de propriedades de nós e arestas (RF20, RF22, RF23).
    Suporta navegação por Tab, fechamento por Esc e inserção de estereótipo «» por Ctrl+Q.
    """

    def __init__(self, parent, element, diagram_type: str, on_apply):
        super().__init__(parent)
        self.element = element
        self.diagram_type = diagram_type
        self.on_apply = on_apply
        self.widgets: dict[str, tuple[str, tk.Widget]] = {}
        self.focus_order: list[tk.Widget] = []

        self.title(f"Propriedades — {element.type}")
        self.transient(parent)
        self.grab_set()
        self.resizable(True, False)

        container = ttk.Frame(self, padding=14)
        container.pack(fill="both", expand=True)

        self._build_fields(container)

        btn_frame = ttk.Frame(container)
        btn_frame.pack(fill="x", pady=(12, 0))
        ok_btn = ttk.Button(btn_frame, text="OK", command=self._save)
        cancel_btn = ttk.Button(btn_frame, text="Cancelar (Esc)", command=self.destroy)
        cancel_btn.pack(side="right", padx=(6, 0))
        ok_btn.pack(side="right")
        self.focus_order.extend([ok_btn, cancel_btn])

        hint = ttk.Label(container, text="Dica: Ctrl+Q insere « » | /itálico/ | _sublinhado_ | Tab navega | Esc fecha", font=("Segoe UI", 8))
        hint.pack(anchor="w", pady=(8, 0))

        self.bind("<Escape>", lambda e: self.destroy())
        self.bind("<Control-q>", self._insert_stereotype)
        self.bind("<Control-Q>", self._insert_stereotype)

        for idx, w in enumerate(self.focus_order):
            w.bind("<Tab>", lambda e, i=idx: self._focus_next(i, 1))
            w.bind("<Shift-Tab>", lambda e, i=idx: self._focus_next(i, -1))
            if isinstance(w, tk.Entry):
                w.bind("<Return>", lambda e: self._save())

        if self.focus_order:
            self.focus_order[0].focus_set()

        self.update_idletasks()
        px = parent.winfo_rootx() + (parent.winfo_width() - self.winfo_width()) // 2
        py = parent.winfo_rooty() + (parent.winfo_height() - self.winfo_height()) // 2
        self.geometry(f"+{max(20, px)}+{max(20, py)}")

    def _focus_next(self, current_idx: int, step: int):
        nxt = (current_idx + step) % len(self.focus_order)
        self.focus_order[nxt].focus_set()
        return "break"

    def _insert_stereotype(self, event=None):
        """RF23: Ctrl+Q insere os delimitadores « » e posiciona o cursor entre eles."""
        w = self.focus_get()
        if isinstance(w, tk.Entry):
            pos = w.index(tk.INSERT)
            w.insert(pos, "«»")
            w.icursor(pos + 1)
            return "break"
        elif isinstance(w, tk.Text):
            w.insert(tk.INSERT, "«»")
            w.mark_set(tk.INSERT, f"{tk.INSERT} - 1 chars")
            return "break"

    def _add_entry(self, parent, key: str, label: str, initial: str):
        ttk.Label(parent, text=label).pack(anchor="w", pady=(4, 1))
        ent = ttk.Entry(parent, width=42)
        ent.insert(0, initial)
        ent.pack(fill="x", pady=(0, 4))
        self.widgets[key] = ("entry", ent)
        self.focus_order.append(ent)

    def _add_text(self, parent, key: str, label: str, initial: str, height: int = 4):
        ttk.Label(parent, text=label).pack(anchor="w", pady=(4, 1))
        txt = tk.Text(parent, width=42, height=height, font=("Segoe UI", 9))
        txt.insert("1.0", initial)
        txt.pack(fill="x", pady=(0, 4))
        self.widgets[key] = ("text", txt)
        self.focus_order.append(txt)

    def _add_combo(self, parent, key: str, label: str, values: list[str], initial: str):
        ttk.Label(parent, text=label).pack(anchor="w", pady=(4, 1))
        cb = ttk.Combobox(parent, values=values, state="readonly", width=39)
        cb.set(initial if initial in values else values[0])
        cb.pack(fill="x", pady=(0, 4))
        self.widgets[key] = ("combo", cb)
        self.focus_order.append(cb)

    def _add_check(self, parent, key: str, label: str, initial: bool):
        var = tk.BooleanVar(value=initial)
        chk = ttk.Checkbutton(parent, text=label, variable=var)
        chk.pack(anchor="w", pady=(6, 4))
        self.widgets[key] = ("check", var)
        self.focus_order.append(chk)

    def _build_fields(self, container):
        el = self.element
        props = el.properties

        if isinstance(el, DiagramNode):
            t = el.type
            if t == "Class":
                self._add_entry(container, "name", "Nome:", props.get("name", ""))
                self._add_text(container, "attributes", "Atributos:", props.get("attributes", ""), 4)
                self._add_text(container, "methods", "Métodos:", props.get("methods", ""), 4)
            elif t == "Interface":
                self._add_entry(container, "name", "Nome:", props.get("name", ""))
                self._add_text(container, "methods", "Métodos:", props.get("methods", ""), 4)
            elif t == "Package":
                self._add_entry(container, "name", "Nome:", props.get("name", ""))
            elif t == "Package Description":
                self._add_entry(container, "name", "Nome:", props.get("name", ""))
                self._add_text(container, "contents", "Conteúdo:", props.get("contents", ""), 4)
            elif t == "Object":
                self._add_entry(container, "name", "Nome:", props.get("name", ""))
            elif t == "Field":
                self._add_entry(container, "name", "Nome:", props.get("name", ""))
                self._add_entry(container, "value", "Valor:", props.get("value", ""))
            elif t in ("Actor", "Use Case", "State", "Implicit Parameter"):
                self._add_entry(container, "name", "Nome:", props.get("name", ""))
            elif t == "Note":
                self._add_text(container, "text", "Texto:", props.get("text", ""), 5)
            elif t == "CallNode":
                self._add_check(container, "open_bottom", "Open Bottom (fluxo incompleto)", bool(props.get("open_bottom", False)))
        else:
            # Propriedades de arestas (RF20)
            t = el.type
            if self.diagram_type == "Class Diagram":
                if t in ("Composition", "Aggregation", "Association"):
                    self._add_entry(container, "start_label", "Start Label:", props.get("start_label", ""))
                    self._add_entry(container, "middle_label", "Middle Label:", props.get("middle_label", ""))
                    self._add_entry(container, "end_label", "End Label:", props.get("end_label", ""))
                    self._add_combo(
                        container,
                        "directionality",
                        "Directionality:",
                        ["Unspecified", "Unidirectional", "Bidirectional"],
                        props.get("directionality", "Unspecified"),
                    )
                    if t in ("Composition", "Aggregation"):
                        self._add_combo(
                            container,
                            "aggregation_type",
                            "Aggregation Type:",
                            ["Aggregation", "Composition"],
                            props.get("aggregation_type", t),
                        )
                elif t in ("Realization", "Inheritance", "Dependency"):
                    self._add_combo(
                        container,
                        "generalization_type",
                        "Generalization Type:",
                        ["Inheritance", "Implementation"],
                        props.get("generalization_type", "Implementation" if t == "Realization" else "Inheritance"),
                    )
                    self._add_combo(
                        container,
                        "directionality",
                        "Directionality:",
                        ["Unidirectional", "Bidirectional"],
                        props.get("directionality", "Unidirectional"),
                    )
            elif self.diagram_type == "Sequence Diagram":
                self._add_entry(container, "middle_label", "Middle Label:", props.get("middle_label", ""))
                if t == "Call":
                    self._add_check(container, "open_bottom", "Open Bottom (fluxo incompleto)", bool(props.get("open_bottom", False)))
            elif self.diagram_type in ("Object Diagram", "State Diagram"):
                self._add_entry(container, "middle_label", "Middle Label:", props.get("middle_label", ""))
            elif self.diagram_type == "Use Case Diagram":
                cur_dep = props.get("dependency_type", t if t in ("Include", "Extend") else "None")
                self._add_combo(
                    container,
                    "dependency_type",
                    "Dependency Type:",
                    ["None", "Include", "Extend"],
                    cur_dep,
                )

    def _save(self):
        new_props = copy.deepcopy(self.element.properties)
        for key, (kind, widget) in self.widgets.items():
            if kind == "entry":
                new_props[key] = widget.get()
            elif kind == "text":
                new_props[key] = widget.get("1.0", "end-1c")
            elif kind == "combo":
                new_props[key] = widget.get()
            elif kind == "check":
                new_props[key] = bool(widget.get())

        # Sincroniza tipo de aresta quando alterado em propriedades (RF20)
        if isinstance(self.element, DiagramEdge):
            if "aggregation_type" in new_props:
                self.element.type = new_props["aggregation_type"]
            if "generalization_type" in new_props and self.element.type in ("Inheritance", "Realization"):
                self.element.type = "Implementation" if new_props["generalization_type"] == "Implementation" else "Inheritance"
                if self.element.type == "Implementation":
                    self.element.type = "Realization"
            if "dependency_type" in new_props and self.diagram_type == "Use Case Diagram":
                dep = new_props["dependency_type"]
                self.element.type = "Association" if dep == "None" else dep

        self.element.properties = new_props
        self.on_apply(self.element)
        self.destroy()


# ==============================================================================
# 8. ABA DE TRABALHO DO DIAGRAMA (CANVAS, INTERAÇÃO, UNDO/REDO, DRAG/LASSO)
# ==============================================================================

class DiagramTab(ttk.Frame):
    """Representa uma aba aberta contendo um diagrama UML e sua barra de ferramentas."""

    def __init__(self, parent, app: "JetUMLApp", model: DiagramModel, filepath: str = None, title_override: str = None):
        super().__init__(parent)
        self.app = app
        self.model = model
        self.filepath = filepath
        self.title_override = title_override
        self.dirty = False

        self.active_tool = "Select"
        self.selected_ids: set[str] = set()
        self.undo_stack: list[dict] = []
        self.redo_stack: list[dict] = []

        # Estados transitórios de mouse
        self.drag_mode = None  # "move", "edge", "lasso"
        self.drag_start = (0.0, 0.0)
        self.drag_last = (0.0, 0.0)
        self.drag_source_node: DiagramNode = None
        self.rubber_line_id = None
        self.lasso_rect_id = None
        self.highlight_pkg_id = None
        self.moved_during_drag = False
        self.pre_drag_snapshot = None

        self.tool_buttons: list[tuple[str, ttk.Button]] = []
        self.tooltip_win = None

        self._build_layout()
        self.push_undo_snapshot(initial=True)
        self.redraw()

    def tab_title(self) -> str:
        base = self.title_override or (os.path.basename(self.filepath) if self.filepath else f"Untitled ({self.model.diagram_type.replace(' Diagram', '')})")
        return f"*{base}" if self.dirty else base

    def _build_layout(self):
        self.sidebar = tk.Frame(self, width=150)
        self.sidebar.pack(side="left", fill="y")

        self.canvas_container = tk.Frame(self)
        self.canvas_container.pack(side="right", fill="both", expand=True)

        self.hbar = ttk.Scrollbar(self.canvas_container, orient="horizontal")
        self.vbar = ttk.Scrollbar(self.canvas_container, orient="vertical")
        self.canvas = tk.Canvas(
            self.canvas_container,
            highlightthickness=0,
            xscrollcommand=self.hbar.set,
            yscrollcommand=self.vbar.set,
        )
        self.hbar.config(command=self.canvas.xview)
        self.vbar.config(command=self.canvas.yview)

        self.hbar.pack(side="bottom", fill="x")
        self.vbar.pack(side="right", fill="y")
        self.canvas.pack(side="left", fill="both", expand=True)

        self.rebuild_toolbar()

        # Eventos do canvas
        self.canvas.bind("<Button-1>", self._on_left_down)
        self.canvas.bind("<B1-Motion>", self._on_left_drag)
        self.canvas.bind("<ButtonRelease-1>", self._on_left_up)
        self.canvas.bind("<Double-Button-1>", self._on_double_click)
        self.canvas.bind("<Button-3>", self._on_right_click)
        self.canvas.bind("<Button-2>", self._on_right_click)

    def rebuild_toolbar(self):
        """Constrói a barra lateral conforme RF07, RF08 e RF10."""
        for w in self.sidebar.winfo_children():
            w.destroy()
        self.tool_buttons.clear()
        theme = self.app.current_theme()
        self.sidebar.configure(bg=theme["toolbar_bg"])

        tools = TOOLS_BY_DIAGRAM[self.model.diagram_type]
        show_hint = self.app.show_tool_hint.get()

        for idx, (name, kind, icon, short_tip, long_tip) in enumerate(tools):
            sc = SHORTCUT_KEYS[idx] if idx < len(SHORTCUT_KEYS) else ""
            label_txt = f"{icon}  {name} [{sc}]" if show_hint else f"{icon}"
            is_active = (name == self.active_tool)
            btn = tk.Button(
                self.sidebar,
                text=label_txt,
                anchor="w" if show_hint else "center",
                relief="solid" if is_active else "flat",
                bd=1 if is_active else 0,
                bg=theme["tool_active"] if is_active else theme["toolbar_bg"],
                fg=theme["fg"],
                activebackground=theme["tool_active"],
                activeforeground=theme["fg"],
                padx=8,
                pady=5,
                font=("Segoe UI", 9, "bold" if is_active else "normal"),
                command=lambda n=name: self.select_tool(n),
            )
            btn.pack(fill="x", padx=4, pady=2)
            btn.bind("<Enter>", lambda e, st=short_tip, lt=long_tip, s=sc: self._show_tooltip(e, st, lt, s))
            btn.bind("<Leave>", lambda e: self._hide_tooltip())
            self.tool_buttons.append((name, btn))

    def _show_tooltip(self, event, short_tip: str, long_tip: str, shortcut: str):
        self._hide_tooltip()
        txt = f"{short_tip} ({shortcut})"
        if self.app.verbose_tooltips.get():
            txt += f"\n{long_tip}"
        tw = tk.Toplevel(self)
        tw.wm_overrideredirect(True)
        tw.wm_geometry(f"+{event.x_root + 14}+{event.y_root + 10}")
        lbl = tk.Label(
            tw,
            text=txt,
            justify="left",
            background="#FFFBEB",
            foreground="#111827",
            relief="solid",
            borderwidth=1,
            padx=8,
            pady=4,
            font=("Segoe UI", 8),
        )
        lbl.pack()
        self.tooltip_win = tw

    def _hide_tooltip(self):
        if self.tooltip_win:
            self.tooltip_win.destroy()
            self.tooltip_win = None

    def select_tool(self, tool_name: str):
        tools = {t[0]: t[1] for t in TOOLS_BY_DIAGRAM[self.model.diagram_type]}
        if tool_name not in tools:
            return
        if tools[tool_name] == "action" and tool_name == "To Clipboard":
            self.app.copy_diagram_to_clipboard()
            return
        self.active_tool = tool_name
        self.rebuild_toolbar()

    def select_tool_by_index(self, idx: int):
        tools = TOOLS_BY_DIAGRAM[self.model.diagram_type]
        if 0 <= idx < len(tools):
            self.select_tool(tools[idx][0])

    # ---------------- Undo / Redo (RF21) ----------------

    def push_undo_snapshot(self, initial: bool = False):
        snap = self.model.to_dict()
        if not initial and self.undo_stack and self.undo_stack[-1] == snap:
            return
        self.undo_stack.append(snap)
        if not initial:
            self.redo_stack.clear()
            self.dirty = True
            self.app.update_tab_titles()

    def undo(self):
        if len(self.undo_stack) <= 1:
            return
        cur = self.undo_stack.pop()
        self.redo_stack.append(cur)
        self.model = DiagramModel.from_dict(self.undo_stack[-1])
        self.selected_ids.clear()
        self.dirty = True
        self.app.update_tab_titles()
        self.redraw()

    def redo(self):
        if not self.redo_stack:
            return
        nxt = self.redo_stack.pop()
        self.undo_stack.append(nxt)
        self.model = DiagramModel.from_dict(nxt)
        self.selected_ids.clear()
        self.dirty = True
        self.app.update_tab_titles()
        self.redraw()

    # ---------------- Coordenadas & Hit Testing ----------------

    def canvas_to_model(self, event) -> tuple[float, float]:
        z = self.app.zoom_factor()
        cx = self.canvas.canvasx(event.x) / z
        cy = self.canvas.canvasy(event.y) / z
        return (cx, cy)

    def hit_test_node(self, mx: float, my: float, prefer_packages: bool = False) -> DiagramNode:
        # Prioriza filhos antes de pacotes (a menos que prefer_packages=True)
        candidates = [n for n in self.model.nodes if n.contains(mx, my)]
        if not candidates:
            return None
        if prefer_packages:
            pkgs = [n for n in candidates if n.type == "Package"]
            return pkgs[-1] if pkgs else None
        non_pkgs = [n for n in candidates if n.type != "Package"]
        if non_pkgs:
            fields = [n for n in non_pkgs if n.type == "Field"]
            return fields[-1] if fields else non_pkgs[-1]
        return candidates[-1]

    def hit_test_edge(self, mx: float, my: float, tol: float = 7.0) -> DiagramEdge:
        for e in reversed(self.model.edges):
            pts = compute_edge_points(self.model, e)
            for i in range(len(pts) - 1):
                if self._point_to_seg_dist(mx, my, pts[i], pts[i + 1]) <= tol:
                    return e
        return None

    @staticmethod
    def _point_to_seg_dist(px, py, p1, p2) -> float:
        x1, y1 = p1
        x2, y2 = p2
        dx, dy = x2 - x1, y2 - y1
        if dx == 0 and dy == 0:
            return math.hypot(px - x1, py - y1)
        t = max(0.0, min(1.0, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)))
        proj_x = x1 + t * dx
        proj_y = y1 + t * dy
        return math.hypot(px - proj_x, py - proj_y)

    # ---------------- Interação de Mouse ----------------

    def _tool_kind(self) -> str:
        for name, kind, *_ in TOOLS_BY_DIAGRAM[self.model.diagram_type]:
            if name == self.active_tool:
                return kind
        return "select"

    def _on_left_down(self, event):
        self.canvas.focus_set()
        mx, my = self.canvas_to_model(event)
        kind = self._tool_kind()
        ctrl_held = bool(event.state & 0x0004)
        shift_held = bool(event.state & 0x0001)

        if kind == "node":
            self._handle_create_node(mx, my)
            return

        if kind == "edge":
            src = self.hit_test_node(mx, my)
            # No diagrama de sequência, permite clicar na linha de vida abaixo do Implicit Parameter
            if src is None and self.model.diagram_type == "Sequence Diagram":
                src = self._hit_lifeline(mx, my)
            if src is None:
                if self.model.diagram_type != "Sequence Diagram":
                    self.app.notify("Selecione um nó válido para iniciar a conexão.", is_error=True)
                return
            self.drag_mode = "edge"
            self.drag_source_node = src
            self.drag_start = (mx, my)
            return

        # Ferramenta Select (RF32)
        hit_n = self.hit_test_node(mx, my)
        hit_e = None if hit_n else self.hit_test_edge(mx, my)
        target = hit_n or hit_e

        if target is not None:
            if ctrl_held:
                if target.id in self.selected_ids:
                    self.selected_ids.remove(target.id)
                else:
                    self.selected_ids.add(target.id)
            else:
                if target.id not in self.selected_ids:
                    self.selected_ids = {target.id}

            # RF15: Shift em nó filho desanexa do pacote imediatamente
            if shift_held and isinstance(target, DiagramNode) and target.parent_id and target.type != "Field":
                target.parent_id = None
                self.push_undo_snapshot()
                self.app.notify("Nó desanexado do pacote.", is_error=False)

            self.drag_mode = "move"
            self.drag_start = (mx, my)
            self.drag_last = (mx, my)
            self.moved_during_drag = False
            self.pre_drag_snapshot = self.model.to_dict()
            self.redraw()
        else:
            if not ctrl_held:
                self.selected_ids.clear()
            self.drag_mode = "lasso"
            self.drag_start = (mx, my)
            self.redraw()

    def _hit_lifeline(self, mx: float, my: float) -> DiagramNode:
        for n in self.model.nodes:
            if n.type == "Implicit Parameter":
                lx = n.x + n.width / 2.0
                if abs(mx - lx) <= 14 and my >= n.y:
                    # Verifica se há um CallNode nesse Y
                    for cn in self.model.nodes:
                        if cn.type == "CallNode" and cn.parent_id == n.id and cn.y <= my <= cn.y + cn.height:
                            return cn
                    return n
        return None

    def _on_left_drag(self, event):
        mx, my = self.canvas_to_model(event)
        z = self.app.zoom_factor()
        theme = self.app.current_theme()

        if self.drag_mode == "edge" and self.drag_source_node:
            if self.rubber_line_id:
                self.canvas.delete(self.rubber_line_id)
            sx, sy = self.drag_start
            self.rubber_line_id = self.canvas.create_line(
                sx * z, sy * z, mx * z, my * z,
                fill=theme["rubber"], width=2, dash=(4, 4)
            )
            return

        if self.drag_mode == "lasso":
            if self.lasso_rect_id:
                self.canvas.delete(self.lasso_rect_id)
            x1, y1 = self.drag_start
            self.lasso_rect_id = self.canvas.create_rectangle(
                x1 * z, y1 * z, mx * z, my * z,
                outline=theme["select"], width=1.5, dash=(4, 3)
            )
            return

        if self.drag_mode == "move" and self.selected_ids:
            dx = snap_val(mx - self.drag_last[0])
            dy = snap_val(my - self.drag_last[1])
            if dx != 0 or dy != 0:
                self.moved_during_drag = True
                self.drag_last = (self.drag_last[0] + dx, self.drag_last[1] + dy)
                nodes_to_move = self._expand_moving_nodes(self.selected_ids)
                for n in nodes_to_move:
                    if n.type == "Field":
                        continue  # Move junto com seu Object pai
                    n.x = max(0, min(self.model.width - n.width, n.x + dx))
                    n.y = max(0, min(self.model.height - n.height, n.y + dy))

                # Destaque visual de pacote ao segurar Shift (RF15)
                shift_held = bool(event.state & 0x0001)
                self.highlight_pkg_id = None
                if shift_held and self.model.diagram_type == "Class Diagram":
                    pkg = self.hit_test_node(mx, my, prefer_packages=True)
                    if pkg and pkg.id not in nodes_to_move:
                        self.highlight_pkg_id = pkg.id
                self.redraw()

    def _expand_moving_nodes(self, root_ids: set[str]) -> set[DiagramNode]:
        result: set[DiagramNode] = set()
        queue = [self.model.get_node(nid) for nid in root_ids if self.model.get_node(nid)]
        while queue:
            cur = queue.pop(0)
            if cur in result:
                continue
            result.add(cur)
            for child in self.model.children_of(cur.id):
                queue.append(child)
        return result

    def _on_left_up(self, event):
        mx, my = self.canvas_to_model(event)
        shift_held = bool(event.state & 0x0001)

        if self.rubber_line_id:
            self.canvas.delete(self.rubber_line_id)
            self.rubber_line_id = None

        if self.lasso_rect_id:
            self.canvas.delete(self.lasso_rect_id)
            self.lasso_rect_id = None

        if self.drag_mode == "edge" and self.drag_source_node:
            self._finish_edge_creation(self.drag_source_node, mx, my)
            self.drag_mode = None
            self.drag_source_node = None
            return

        if self.drag_mode == "lasso":
            x1, y1 = min(self.drag_start[0], mx), min(self.drag_start[1], my)
            x2, y2 = max(self.drag_start[0], mx), max(self.drag_start[1], my)
            for n in self.model.nodes:
                nx1, ny1, nx2, ny2 = n.bounds()
                if nx2 >= x1 and nx1 <= x2 and ny2 >= y1 and ny1 <= y2:
                    self.selected_ids.add(n.id)
            for e in self.model.edges:
                pts = compute_edge_points(self.model, e)
                if any(x1 <= px <= x2 and y1 <= py <= y2 for px, py in pts):
                    self.selected_ids.add(e.id)
            self.drag_mode = None
            self.redraw()
            return

        if self.drag_mode == "move":
            # RF15: Anexar ao pacote ao soltar com Shift pressionado
            if shift_held and self.model.diagram_type == "Class Diagram":
                pkg = self.hit_test_node(mx, my, prefer_packages=True)
                if pkg:
                    attached = False
                    for nid in list(self.selected_ids):
                        nd = self.model.get_node(nid)
                        if nd and nd.id != pkg.id and nd.type != "PointNode":
                            nd.parent_id = pkg.id
                            attached = True
                    if attached:
                        self.moved_during_drag = True
                        self.app.notify("Nó anexado ao pacote.", is_error=False)
            self.highlight_pkg_id = None
            if self.moved_during_drag:
                self.push_undo_snapshot()
            self.drag_mode = None
            self.redraw()

    # ---------------- Criação de Nós (RF11, RF12, RF13, RF15, RF25) ----------------

    def _handle_create_node(self, mx: float, my: float):
        sx, sy = snap_val(mx), snap_val(my)
        t = self.active_tool

        # RF25: Field deve pertencer obrigatoriamente a um Object
        if t == "Field":
            obj = self.hit_test_node(mx, my)
            if obj is None or obj.type != "Object":
                self.app.notify("Um Campo (Field) deve ser criado dentro de um Objeto.", is_error=True)
                return
            node = DiagramNode("Field", obj.x + 8, obj.y + obj.height, parent_id=obj.id)
            self.model.nodes.append(node)
            self.selected_ids = {node.id}
            self.push_undo_snapshot()
            self.redraw()
            if self.app.auto_edit_node.get():
                self.open_properties_for(node)
            return

        # RF15: Clicar dentro de um Package cria o nó diretamente como filho
        parent_pkg = None
        if self.model.diagram_type == "Class Diagram":
            parent_pkg = self.hit_test_node(mx, my, prefer_packages=True)

        node = DiagramNode(t, sx, sy, parent_id=parent_pkg.id if parent_pkg else None)
        if node.x + node.width > self.model.width:
            node.x = max(0, self.model.width - node.width)
        if node.y + node.height > self.model.height:
            node.y = max(0, self.model.height - node.height)

        self.model.nodes.append(node)
        self.selected_ids = {node.id}
        self.push_undo_snapshot()
        self.redraw()

        # RF12: Auto Edit Node abre propriedades automaticamente (exceto Initial/Final State sem propriedades)
        if self.app.auto_edit_node.get() and node.type not in ("Initial State", "Final State"):
            self.open_properties_for(node)

    # ---------------- Criação de Arestas & Sequência (RF14, RF16–RF19, RF26–RF30) ----------------

    def _finish_edge_creation(self, src: DiagramNode, mx: float, my: float):
        edge_type = self.active_tool
        dst = self.hit_test_node(mx, my)
        if dst is None and self.model.diagram_type == "Sequence Diagram":
            dst = self._hit_lifeline(mx, my)

        # RF14: Note Connector arrastado da Nota até um ponto vazio ancora no ponto fixo
        if edge_type == "Note Connector" and src.type == "Note" and dst is None:
            anchor = DiagramNode("PointNode", snap_val(mx) - 4, snap_val(my) - 4)
            self.model.nodes.append(anchor)
            edge = DiagramEdge("Note Connector", src.id, anchor.id)
            self.model.edges.append(edge)
            self.selected_ids = {edge.id}
            self.push_undo_snapshot()
            self.redraw()
            return

        if dst is None:
            if self.model.diagram_type != "Sequence Diagram":
                self.app.notify("Solte a conexão sobre um elemento válido.", is_error=True)
            self.redraw()
            return

        # RF26: Reference em Object Diagram deve partir da área Value (metade direita) do Field
        if edge_type == "Reference":
            if src.type == "Field":
                val_x = src.x + src.width * 0.52
                if self.drag_start[0] < val_x:
                    self.app.notify("Arraste a referência a partir da área de Valor do Campo.", is_error=True)
                    self.redraw()
                    return

        # Fluxo especial do Diagrama de Sequência (RF27, RF28, RF29, RF30)
        if self.model.diagram_type == "Sequence Diagram" and edge_type in ("Call", "Return"):
            self._handle_sequence_edge(edge_type, src, dst, mx, my)
            self.redraw()
            return

        val = validate_edge_creation(self.model, edge_type, src, dst)
        if not val.valid:
            if not val.silent and val.message:
                self.app.notify(val.message, is_error=True)
            self.redraw()
            return

        edge = DiagramEdge(edge_type, src.id, dst.id)
        self.model.edges.append(edge)
        self.selected_ids = {edge.id}
        self.push_undo_snapshot()
        self.redraw()

    def _handle_sequence_edge(self, edge_type: str, src: DiagramNode, dst: DiagramNode, mx: float, my: float):
        """Implementa RF27, RF28, RF29 e validação silenciosa RF30 no Diagrama de Sequência."""
        call_y = snap_val(self.drag_start[1])

        if edge_type == "Return":
            # Retorno exige origem e destino como caixas de ativação (CallNode)
            if src.type != "CallNode" or dst.type != "CallNode" or src.id == dst.id:
                return  # RF30: falha silenciosamente
            # Verifica se dst é de fato o chamador de src
            is_caller = any(e.type == "Call" and e.source_id == dst.id and e.target_id == src.id for e in self.model.edges)
            if not is_caller:
                return  # RF30: falha silenciosamente
            edge = DiagramEdge("Return", src.id, dst.id)
            self.model.edges.append(edge)
            self.selected_ids = {edge.id}
            self.push_undo_snapshot()
            return

        # edge_type == "Call"
        # RF29: Chamada de construtor quando destino é a caixa superior de um Implicit Parameter
        if dst.type == "Implicit Parameter" and dst.contains(mx, my):
            if src.type not in ("Implicit Parameter", "CallNode"):
                return
            # Não permite construir o mesmo participante duas vezes
            if any(e.type == "Call" and e.target_id == dst.id for e in self.model.edges):
                return
            caller_cn = self._ensure_caller_activation(src, call_y)
            if caller_cn is None:
                return
            # Alinha verticalmente o parâmetro implícito à chamada (RF29)
            dst.y = max(20, snap_val(call_y - dst.height / 2.0))
            # Anexa caixa de ativação ao parâmetro criado (RF29)
            callee_cn = DiagramNode("CallNode", dst.x + dst.width / 2.0 - 8, dst.y + dst.height, parent_id=dst.id, height=50)
            self.model.nodes.append(callee_cn)
            edge = DiagramEdge("Call", caller_cn.id, dst.id, properties={"middle_label": "«create»", "open_bottom": False, "is_create": True})
            self.model.edges.append(edge)
            self.selected_ids = {edge.id}
            self.push_undo_snapshot()
            return

        # Determina participante de origem e destino
        src_param = src if src.type == "Implicit Parameter" else self.model.get_node(src.parent_id)
        dst_param = dst if dst.type == "Implicit Parameter" else self.model.get_node(dst.parent_id)
        if not src_param or not dst_param:
            return

        # RF28: Autochamada (arrastar dentro de uma caixa de ativação ou mesma linha de vida)
        if src_param.id == dst_param.id:
            if src.type != "CallNode":
                return  # RF30: autochamada deve partir de uma caixa de ativação existente
            nested_cn = DiagramNode(
                "CallNode",
                src.x + 8,
                max(src.y + 16, call_y),
                parent_id=src_param.id,
                height=36,
                properties={"open_bottom": False, "nest_depth": src.properties.get("nest_depth", 0) + 1},
            )
            src.height = max(src.height, (nested_cn.y - src.y) + nested_cn.height + 12)
            self.model.nodes.append(nested_cn)
            edge = DiagramEdge("Call", src.id, nested_cn.id)
            self.model.edges.append(edge)
            self.selected_ids = {edge.id}
            self.push_undo_snapshot()
            return

        # RF30: Impede que um bloco de ativação existente receba chamadas de múltiplos chamadores simultâneos
        if dst.type == "CallNode":
            existing_callers = {e.source_id for e in self.model.edges if e.type == "Call" and e.target_id == dst.id}
            if existing_callers and src.id not in existing_callers:
                return  # RF30: falha silenciosamente

        caller_cn = self._ensure_caller_activation(src, call_y)
        if caller_cn is None:
            return

        callee_y = max(dst_param.y + dst_param.height + 10, call_y)
        callee_cn = DiagramNode("CallNode", dst_param.x + dst_param.width / 2.0 - 8, callee_y, parent_id=dst_param.id, height=44)
        self.model.nodes.append(callee_cn)
        edge = DiagramEdge("Call", caller_cn.id, callee_cn.id)
        self.model.edges.append(edge)
        self.selected_ids = {edge.id}
        self.push_undo_snapshot()

    def _ensure_caller_activation(self, src: DiagramNode, call_y: float) -> DiagramNode:
        """Garante RF30: no máximo um bloco raiz órfão (sem chamador) por diagrama."""
        if src.type == "CallNode":
            if call_y > src.y + src.height - 10:
                src.height = (call_y - src.y) + 30
            return src
        if src.type == "Implicit Parameter":
            # Verifica se já existe um bloco raiz órfão no diagrama (RF30)
            all_call_nodes = [n for n in self.model.nodes if n.type == "CallNode"]
            targeted_ids = {e.target_id for e in self.model.edges if e.type == "Call"}
            orphan_roots = [cn for cn in all_call_nodes if cn.id not in targeted_ids]
            if orphan_roots:
                return None  # RF30: impede mais de um bloco raiz órfão
            root_cn = DiagramNode(
                "CallNode",
                src.x + src.width / 2.0 - 8,
                max(src.y + src.height + 10, call_y),
                parent_id=src.id,
                height=50,
            )
            self.model.nodes.append(root_cn)
            return root_cn
        return None

    # ---------------- Propriedades, Menu de Contexto & Edição ----------------

    def _on_double_click(self, event):
        mx, my = self.canvas_to_model(event)
        target = self.hit_test_node(mx, my) or self.hit_test_edge(mx, my)
        if target:
            self.selected_ids = {target.id}
            self.redraw()
            self.open_properties_for(target)

    def open_selected_properties(self):
        if len(self.selected_ids) != 1:
            return
        eid = next(iter(self.selected_ids))
        el = self.model.get_node(eid) or self.model.get_edge(eid)
        if el:
            self.open_properties_for(el)

    def open_properties_for(self, element):
        if isinstance(element, DiagramNode) and element.type in ("Initial State", "Final State", "PointNode"):
            self.app.notify("Este elemento não possui propriedades editáveis.", is_error=False)
            return
        if isinstance(element, DiagramEdge) and element.type in ("Note Connector", "Reference"):
            self.app.notify("Este conector não possui propriedades editáveis.", is_error=False)
            return

        def apply_cb(updated_el):
            # RF31: sincroniza Open Bottom entre aresta Call e sua caixa de ativação destino
            if isinstance(updated_el, DiagramEdge) and updated_el.type == "Call":
                ob = bool(updated_el.properties.get("open_bottom", False))
                tgt = self.model.get_node(updated_el.target_id)
                if tgt and tgt.type == "CallNode":
                    tgt.properties["open_bottom"] = ob
            self.push_undo_snapshot()
            self.redraw()

        PropertiesDialog(self.app.root, element, self.model.diagram_type, apply_cb)

    def _on_right_click(self, event):
        """RF09: Clicar com o botão direito exibe as ferramentas da barra em menu popup."""
        menu = tk.Menu(self, tearoff=0)
        for idx, (name, kind, icon, _, _) in enumerate(TOOLS_BY_DIAGRAM[self.model.diagram_type]):
            sc = f" ({SHORTCUT_KEYS[idx]})" if idx < len(SHORTCUT_KEYS) else ""
            menu.add_command(label=f"{icon}  {name}{sc}", command=lambda n=name: self.select_tool(n))
        menu.tk_popup(event.x_root, event.y_root)

    # ---------------- Seleção, Exclusão, Copiar/Colar (RF32–RF35) ----------------

    def select_all(self):
        self.selected_ids = {n.id for n in self.model.nodes} | {e.id for e in self.model.edges}
        self.redraw()

    def delete_selected(self):
        if not self.selected_ids:
            return
        to_remove_nodes = {n.id for n in self._expand_moving_nodes(self.selected_ids)}
        self.model.nodes = [n for n in self.model.nodes if n.id not in to_remove_nodes]
        self.model.edges = [
            e for e in self.model.edges
            if e.id not in self.selected_ids
            and e.source_id not in to_remove_nodes
            and e.target_id not in to_remove_nodes
        ]
        self.selected_ids.clear()
        self.push_undo_snapshot()
        self.redraw()

    # ---------------- Renderização no Canvas Tkinter ----------------

    def redraw(self):
        self.canvas.delete("all")
        theme = self.app.current_theme()
        z = self.app.zoom_factor()
        ff = self.app.diagram_font_family.get()
        fs = self.app.diagram_font_size.get()

        self.canvas.configure(bg=theme["canvas_outer"])
        w_scaled = int(self.model.width * z)
        h_scaled = int(self.model.height * z)
        self.canvas.configure(scrollregion=(0, 0, w_scaled + 40, h_scaled + 40))

        cmds = build_scene_commands(
            self.model,
            theme,
            selected_ids=self.selected_ids,
            include_grid=self.app.show_grid.get(),
            highlight_pkg_id=self.highlight_pkg_id,
            font_family=ff,
            font_size=fs,
        )

        for c in cmds:
            d = c.data
            if c.kind == "rect":
                self.canvas.create_rectangle(
                    d["x1"] * z, d["y1"] * z, d["x2"] * z, d["y2"] * z,
                    fill=d["fill"], outline=d["outline"], width=d.get("width", 1) * z
                )
            elif c.kind == "round_rect":
                self._draw_round_rect(
                    d["x1"] * z, d["y1"] * z, d["x2"] * z, d["y2"] * z,
                    d.get("radius", 14) * z, fill=d["fill"], outline=d["outline"], width=d.get("width", 1) * z
                )
            elif c.kind == "oval":
                self.canvas.create_oval(
                    d["x1"] * z, d["y1"] * z, d["x2"] * z, d["y2"] * z,
                    fill=d["fill"], outline=d["outline"], width=d.get("width", 1) * z
                )
            elif c.kind == "polygon":
                flat = [coord * z for pt in d["points"] for coord in pt]
                self.canvas.create_polygon(flat, fill=d["fill"], outline=d["outline"], width=d.get("width", 1) * z)
            elif c.kind == "line":
                flat = [coord * z for pt in d["points"] for coord in pt]
                dash = tuple(max(1, int(v * z)) for v in d["dash"]) if d.get("dash") else ()
                self.canvas.create_line(flat, fill=d["fill"], width=d.get("width", 1) * z, dash=dash)
            elif c.kind == "rich_text":
                self._draw_rich_text(
                    d["x"] * z, d["y"] * z, d["text"], d["anchor"],
                    d.get("bold", False), d["fill"], d["font_family"], max(6, int(round(d["font_size"] * z)))
                )

    def _draw_round_rect(self, x1, y1, x2, y2, r, **kwargs):
        pts = [
            x1 + r, y1,
            x2 - r, y1,
            x2, y1,
            x2, y1 + r,
            x2, y2 - r,
            x2, y2,
            x2 - r, y2,
            x1 + r, y2,
            x1, y2,
            x1, y2 - r,
            x1, y1 + r,
            x1, y1,
        ]
        self.canvas.create_polygon(pts, smooth=True, **kwargs)

    def _draw_rich_text(self, x: float, y: float, text: str, anchor: str, bold: bool, fill: str, family: str, size: int):
        """Renderiza segmentos com /itálico/ e _sublinhado_ (RF24)."""
        segs = parse_formatted_line(text)
        measured = []
        total_w = 0
        for seg_txt, is_it, is_ul in segs:
            fnt = tkfont.Font(
                family=family,
                size=size,
                weight="bold" if bold else "normal",
                slant="italic" if is_it else "roman",
                underline=1 if is_ul else 0,
            )
            w = fnt.measure(seg_txt)
            measured.append((seg_txt, fnt, w))
            total_w += w

        if anchor == "center":
            cur_x = x - total_w / 2.0
        elif anchor == "e":
            cur_x = x - total_w
        else:
            cur_x = x

        for seg_txt, fnt, w in measured:
            if seg_txt:
                self.canvas.create_text(cur_x, y, text=seg_txt, anchor="w", fill=fill, font=fnt)
            cur_x += w


# ==============================================================================
# 9. APLICAÇÃO PRINCIPAL (MENUS, WELCOME SCREEN, ABAS, ZOOM, TOASTS)
# ==============================================================================

class JetUMLApp:
    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title(APP_TITLE)

        # RF38 & RNF01: Tamanho inicial proporcional à tela (faixa 250..4000)
        sw = self.root.winfo_screenwidth()
        sh = self.root.winfo_screenheight()
        self.default_diag_w = max(250, min(4000, int(sw * 0.72)))
        self.default_diag_h = max(250, min(4000, int(sh * 0.72)))
        self.diag_width = self.default_diag_w
        self.diag_height = self.default_diag_h

        self.root.geometry(f"{min(sw - 60, self.default_diag_w + 180)}x{min(sh - 80, self.default_diag_h + 90)}")

        # Estados de visualização (RF10, RF12, RF36, RF37, RF39, RF40, RF42)
        self.show_tool_hint = tk.BooleanVar(value=True)
        self.verbose_tooltips = tk.BooleanVar(value=False)
        self.auto_edit_node = tk.BooleanVar(value=False)
        self.show_grid = tk.BooleanVar(value=True)
        self.dark_mode = tk.BooleanVar(value=False)
        self.zoom_index = 2  # 100%
        self.diagram_font_family = tk.StringVar(value="Segoe UI")
        self.diagram_font_size = tk.IntVar(value=10)
        self.notification_duration = tk.IntVar(value=5)

        self.clipboard_payload: dict = None
        self.recent_files: list[str] = self._load_recent_files()
        self.toast_label: tk.Label = None
        self.toast_after_id = None

        self._build_menus()
        self._build_main_area()
        self._bind_global_shortcuts()
        self.apply_theme()

        self.root.protocol("WM_DELETE_WINDOW", self.exit_app)

    def current_theme(self) -> dict:
        return THEMES["dark" if self.dark_mode.get() else "light"]

    def zoom_factor(self) -> float:
        return ZOOM_LEVELS[self.zoom_index]

    # ---------------- Menus (RF01–RF06, RF10, RF12, RF21–RF22, RF33–RF42) ----------------

    def _build_menus(self):
        self.menubar = tk.Menu(self.root)

        # File Menu
        file_menu = tk.Menu(self.menubar, tearoff=0)
        new_menu = tk.Menu(file_menu, tearoff=0)
        for dt in DIAGRAM_TYPES:
            new_menu.add_command(label=dt, command=lambda d=dt: self.new_diagram(d))
        file_menu.add_cascade(label="New", menu=new_menu)
        file_menu.add_command(label="Open...", accelerator="Ctrl+O", command=self.open_diagram)

        self.recent_menu = tk.Menu(file_menu, tearoff=0)
        file_menu.add_cascade(label="Recent Files", menu=self.recent_menu)
        self._refresh_recent_menu()

        file_menu.add_separator()
        file_menu.add_command(label="Close", accelerator="Ctrl+W", command=self.close_current_tab)
        file_menu.add_command(label="Save", accelerator="Ctrl+S", command=self.save_diagram)
        file_menu.add_command(label="Save As...", command=self.save_diagram_as)
        file_menu.add_command(label="Duplicate", accelerator="Ctrl+D", command=self.duplicate_diagram)
        file_menu.add_separator()
        file_menu.add_command(label="Export Image...", command=self.export_image)
        file_menu.add_command(label="Copy to Clipboard", accelerator="Ctrl+B", command=self.copy_diagram_to_clipboard)
        file_menu.add_separator()
        file_menu.add_command(label="Exit", accelerator="Ctrl+Q", command=self.exit_app)
        self.menubar.add_cascade(label="File", menu=file_menu)

        # Edit Menu
        edit_menu = tk.Menu(self.menubar, tearoff=0)
        edit_menu.add_command(label="Undo", accelerator="Ctrl+Z", command=lambda: self._call_tab("undo"))
        edit_menu.add_command(label="Redo", accelerator="Ctrl+Y", command=lambda: self._call_tab("redo"))
        edit_menu.add_separator()
        edit_menu.add_command(label="Select All", accelerator="Ctrl+A", command=lambda: self._call_tab("select_all"))
        edit_menu.add_command(label="Properties", accelerator="Ctrl+Enter", command=lambda: self._call_tab("open_selected_properties"))
        edit_menu.add_separator()
        edit_menu.add_command(label="Cut", accelerator="Ctrl+X", command=self.cut_selection)
        edit_menu.add_command(label="Copy", accelerator="Ctrl+C", command=self.copy_selection)
        edit_menu.add_command(label="Paste", accelerator="Ctrl+V", command=self.paste_selection)
        edit_menu.add_command(label="Delete", accelerator="Delete", command=lambda: self._call_tab("delete_selected"))
        self.menubar.add_cascade(label="Edit", menu=edit_menu)

        # View Menu
        view_menu = tk.Menu(self.menubar, tearoff=0)
        view_menu.add_checkbutton(label="Show Tool Hint", variable=self.show_tool_hint, command=self._refresh_all_toolbars)
        view_menu.add_checkbutton(label="Verbose Tooltips", variable=self.verbose_tooltips)
        view_menu.add_checkbutton(label="Auto Edit Node", variable=self.auto_edit_node)
        view_menu.add_separator()
        view_menu.add_checkbutton(label="Show Grid", variable=self.show_grid, command=self._redraw_all_tabs)
        view_menu.add_command(label="Zoom In", accelerator="Ctrl+=", command=self.zoom_in)
        view_menu.add_command(label="Zoom Out", accelerator="Ctrl+-", command=self.zoom_out)
        view_menu.add_command(label="Reset Zoom", accelerator="Ctrl+0", command=self.reset_zoom)
        view_menu.add_separator()
        view_menu.add_command(label="Set Diagram Size...", command=self.open_diagram_size_dialog)
        view_menu.add_command(label="Change Font...", command=self.open_font_dialog)
        view_menu.add_checkbutton(label="Dark Mode", variable=self.dark_mode, command=self.apply_theme)
        view_menu.add_command(label="Notification Duration...", command=self.open_notification_duration_dialog)
        self.menubar.add_cascade(label="View", menu=view_menu)

        self.root.config(menu=self.menubar)

    def _build_main_area(self):
        self.main_stack = tk.Frame(self.root)
        self.main_stack.pack(fill="both", expand=True)

        # Tela de Boas-Vindas (RF01)
        self.welcome_frame = tk.Frame(self.main_stack)
        self._build_welcome_screen()

        # Notebook de Abas (RF01)
        self.notebook = ttk.Notebook(self.main_stack)
        self.notebook.bind("<<NotebookTabChanged>>", lambda e: self.update_tab_titles())

        self._show_welcome_or_tabs()

    def _build_welcome_screen(self):
        for w in self.welcome_frame.winfo_children():
            w.destroy()
        theme = self.current_theme()
        self.welcome_frame.configure(bg=theme["bg"])

        box = tk.Frame(self.welcome_frame, bg=theme["panel_bg"], padx=36, pady=30, relief="solid", bd=1)
        box.place(relx=0.5, rely=0.5, anchor="center")

        tk.Label(box, text="JetUML", font=("Segoe UI", 24, "bold"), bg=theme["panel_bg"], fg=theme["fg"]).pack(pady=(0, 4))
        tk.Label(box, text="Selecione um tipo de diagrama UML para começar:", font=("Segoe UI", 10), bg=theme["panel_bg"], fg=theme["muted_fg"]).pack(pady=(0, 18))

        for dt in DIAGRAM_TYPES:
            btn = tk.Button(
                box,
                text=f"＋  Novo {dt}",
                width=32,
                pady=7,
                bg=theme["accent"],
                fg=theme["accent_fg"],
                activebackground=theme["tool_active"],
                relief="flat",
                font=("Segoe UI", 10, "bold"),
                command=lambda d=dt: self.new_diagram(d),
            )
            btn.pack(pady=4)

        ttk.Separator(box, orient="horizontal").pack(fill="x", pady=14)
        tk.Button(
            box,
            text="📂  Abrir Arquivo JSON (Ctrl+O)",
            width=32,
            pady=6,
            bg=theme["toolbar_bg"],
            fg=theme["fg"],
            relief="flat",
            font=("Segoe UI", 9),
            command=self.open_diagram,
        ).pack(pady=2)

    def _show_welcome_or_tabs(self):
        if len(self.notebook.tabs()) == 0:
            self.notebook.pack_forget()
            self._build_welcome_screen()
            self.welcome_frame.pack(fill="both", expand=True)
        else:
            self.welcome_frame.pack_forget()
            self.notebook.pack(fill="both", expand=True)

    # ---------------- Atalhos de Teclado Globais (RF02–RF08, RF21–RF22, RF33–RF37) ----------------

    def _bind_global_shortcuts(self):
        self.root.bind_all("<Control-o>", lambda e: self.open_diagram())
        self.root.bind_all("<Control-w>", lambda e: self.close_current_tab())
        self.root.bind_all("<Control-s>", lambda e: self.save_diagram())
        self.root.bind_all("<Control-d>", lambda e: self.duplicate_diagram())
        self.root.bind_all("<Control-b>", lambda e: self.copy_diagram_to_clipboard())
        self.root.bind_all("<Control-q>", self._on_ctrl_q)
        self.root.bind_all("<Control-z>", lambda e: self._call_tab("undo"))
        self.root.bind_all("<Control-y>", lambda e: self._call_tab("redo"))
        self.root.bind_all("<Control-a>", self._on_ctrl_a)
        self.root.bind_all("<Control-Return>", lambda e: self._call_tab("open_selected_properties"))
        self.root.bind_all("<Control-x>", lambda e: self.cut_selection())
        self.root.bind_all("<Control-c>", lambda e: self.copy_selection())
        self.root.bind_all("<Control-v>", lambda e: self.paste_selection())
        self.root.bind_all("<Delete>", self._on_delete_key)
        self.root.bind_all("<Control-equal>", lambda e: self.zoom_in())
        self.root.bind_all("<Control-plus>", lambda e: self.zoom_in())
        self.root.bind_all("<Control-minus>", lambda e: self.zoom_out())
        self.root.bind_all("<Control-0>", lambda e: self.reset_zoom())
        self.root.bind("<KeyPress>", self._on_tool_shortcut_key)

    def _is_text_input_focused(self) -> bool:
        w = self.root.focus_get()
        return isinstance(w, (tk.Entry, tk.Text, ttk.Entry, ttk.Spinbox))

    def _on_ctrl_q(self, event):
        # Se estiver dentro do diálogo de propriedades, o diálogo trata Ctrl+Q para «» (RF23)
        if isinstance(self.root.focus_get(), (tk.Entry, tk.Text)):
            return
        self.exit_app()

    def _on_ctrl_a(self, event):
        if self._is_text_input_focused():
            return
        self._call_tab("select_all")
        return "break"

    def _on_delete_key(self, event):
        if self._is_text_input_focused():
            return
        self._call_tab("delete_selected")

    def _on_tool_shortcut_key(self, event):
        """RF08: Atalhos 1-9, 0, A, B... selecionam ferramentas da barra lateral."""
        if self._is_text_input_focused():
            return
        if event.state & (0x0004 | 0x0008 | 0x20000):  # Ignora Ctrl/Alt
            return
        tab = self.current_tab()
        if not tab or not event.char:
            return
        ch = event.char.upper()
        if ch in SHORTCUT_KEYS:
            tab.select_tool_by_index(SHORTCUT_KEYS.index(ch))

    # ---------------- Gerenciamento de Abas & Arquivos (RF01–RF06) ----------------

    def current_tab(self) -> DiagramTab:
        sel = self.notebook.select()
        if not sel:
            return None
        return self.root.nametowidget(sel)

    def _call_tab(self, method_name: str):
        tab = self.current_tab()
        if tab and hasattr(tab, method_name):
            getattr(tab, method_name)()

    def update_tab_titles(self):
        for tid in self.notebook.tabs():
            tab: DiagramTab = self.root.nametowidget(tid)
            self.notebook.tab(tid, text=tab.tab_title())

    def new_diagram(self, diagram_type: str):
        model = DiagramModel(diagram_type, width=self.diag_width, height=self.diag_height)
        tab = DiagramTab(self.notebook, self, model)
        self.notebook.add(tab, text=tab.tab_title())
        self.notebook.select(tab)
        self._show_welcome_or_tabs()

    def open_diagram(self, path: str = None):
        if not path:
            path = filedialog.askopenfilename(
                title="Abrir Diagrama JetUML",
                filetypes=[("JetUML JSON", "*.json"), ("Todos os arquivos", "*.*")],
            )
        if not path:
            return
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
            model = DiagramModel.from_dict(data)
            # RF38: Ao abrir diagrama maior que a área configurada, aumenta automaticamente
            if model.width < self.diag_width:
                model.width = self.diag_width
            if model.height < self.diag_height:
                model.height = self.diag_height
            if model.width > self.diag_width or model.height > self.diag_height:
                self.diag_width = max(self.diag_width, min(4000, model.width))
                self.diag_height = max(self.diag_height, min(4000, model.height))

            tab = DiagramTab(self.notebook, self, model, filepath=path)
            self.notebook.add(tab, text=tab.tab_title())
            self.notebook.select(tab)
            self._show_welcome_or_tabs()
            self._add_recent_file(path)
            self.notify(f"Diagrama aberto: {os.path.basename(path)}", is_error=False)
        except Exception as exc:
            self.notify(f"Erro ao abrir arquivo: {exc}", is_error=True)

    def save_diagram(self) -> bool:
        tab = self.current_tab()
        if not tab:
            return False
        if not tab.filepath:
            return self.save_diagram_as()
        try:
            with open(tab.filepath, "w", encoding="utf-8") as f:
                json.dump(tab.model.to_dict(), f, indent=2, ensure_ascii=False)
            tab.dirty = False
            self.update_tab_titles()
            self._add_recent_file(tab.filepath)
            self.notify("Diagrama salvo com sucesso.", is_error=False)
            return True
        except Exception as exc:
            self.notify(f"Falha ao salvar: {exc}", is_error=True)
            return False

    def save_diagram_as(self) -> bool:
        tab = self.current_tab()
        if not tab:
            return False
        path = filedialog.asksaveasfilename(
            title="Salvar Diagrama Como",
            defaultextension=".json",
            filetypes=[("JetUML JSON", "*.json")],
        )
        if not path:
            return False
        tab.filepath = path
        tab.title_override = None
        return self.save_diagram()

    def close_current_tab(self):
        tab = self.current_tab()
        if not tab:
            return
        if tab.dirty:
            res = messagebox.askyesnocancel("Alterações não salvas", f"Deseja salvar '{tab.tab_title().lstrip('*')}' antes de fechar?")
            if res is None:
                return
            if res is True and not self.save_diagram():
                return
        self.notebook.forget(tab)
        tab.destroy()
        self._show_welcome_or_tabs()

    def duplicate_diagram(self):
        """RF03: File > Duplicate (Ctrl+D) cria uma cópia do diagrama atual em nova aba."""
        tab = self.current_tab()
        if not tab:
            return
        cloned_model = DiagramModel.from_dict(tab.model.to_dict())
        base_name = os.path.basename(tab.filepath) if tab.filepath else tab.model.diagram_type
        dup_tab = DiagramTab(self.notebook, self, cloned_model, title_override=f"{base_name} (Copy)")
        self.notebook.add(dup_tab, text=dup_tab.tab_title())
        self.notebook.select(dup_tab)
        self.notify("Diagrama duplicado em nova aba.", is_error=False)

    def export_image(self):
        """RF04: File > Export Image salva o diagrama em SVG, PNG, JPEG ou PPM."""
        tab = self.current_tab()
        if not tab:
            return
        path = filedialog.asksaveasfilename(
            title="Exportar Diagrama como Imagem",
            defaultextension=".png",
            filetypes=[
                ("PNG Image", "*.png"),
                ("SVG Vector Image", "*.svg"),
                ("JPEG Image", "*.jpg;*.jpeg"),
                ("PPM Image", "*.ppm"),
            ],
        )
        if not path:
            return
        try:
            ext = os.path.splitext(path)[1].lower()
            theme = self.current_theme()
            ff = self.diagram_font_family.get()
            fs = self.diagram_font_size.get()
            if ext == ".svg":
                export_diagram_svg(tab.model, path, theme, ff, fs, crop=False)
            else:
                render_diagram_raster(tab.model, path, theme, ff, fs, crop=False)
            self.notify(f"Imagem exportada: {os.path.basename(path)}", is_error=False)
        except Exception as exc:
            self.notify(f"Erro ao exportar imagem: {exc}", is_error=True)

    def copy_diagram_to_clipboard(self):
        """RF05: Copia imagem do diagrama inteiro, sem grade e ajustada aos limites do conteúdo."""
        tab = self.current_tab()
        if not tab:
            return
        try:
            tmp_png = os.path.join(tempfile.gettempdir(), "jetuml_clipboard.png")
            render_diagram_raster(
                tab.model,
                tmp_png,
                self.current_theme(),
                self.diagram_font_family.get(),
                self.diagram_font_size.get(),
                crop=True,  # Sem grade e ajustada aos limites
            )
            copy_image_to_system_clipboard(self.root, tmp_png)
            self.notify("Imagem do diagrama copiada para a área de transferência.", is_error=False)
        except Exception as exc:
            self.notify(f"Erro ao copiar imagem: {exc}", is_error=True)

    def exit_app(self):
        """RF06: Encerra a aplicação."""
        for tid in list(self.notebook.tabs()):
            tab: DiagramTab = self.root.nametowidget(tid)
            if tab.dirty:
                self.notebook.select(tid)
                res = messagebox.askyesnocancel("Sair", f"Salvar alterações em '{tab.tab_title().lstrip('*')}' antes de sair?")
                if res is None:
                    return
                if res is True and not self.save_diagram():
                    return
        self.root.destroy()

    # ---------------- Recent Files (RF02) ----------------

    def _load_recent_files(self) -> list[str]:
        try:
            if os.path.exists(RECENT_FILES_PATH):
                with open(RECENT_FILES_PATH, "r", encoding="utf-8") as f:
                    return [p for p in json.load(f) if os.path.exists(p)][:8]
        except Exception:
            pass
        return []

    def _add_recent_file(self, path: str):
        abs_p = os.path.abspath(path)
        if abs_p in self.recent_files:
            self.recent_files.remove(abs_p)
        self.recent_files.insert(0, abs_p)
        self.recent_files = self.recent_files[:8]
        try:
            with open(RECENT_FILES_PATH, "w", encoding="utf-8") as f:
                json.dump(self.recent_files, f, indent=2)
        except Exception:
            pass
        self._refresh_recent_menu()

    def _refresh_recent_menu(self):
        self.recent_menu.delete(0, "end")
        if not self.recent_files:
            self.recent_menu.add_command(label="(Vazio)", state="disabled")
            return
        for p in self.recent_files:
            self.recent_menu.add_command(label=p, command=lambda fp=p: self.open_diagram(fp))

    # ---------------- Cut / Copy / Paste de Elementos (RF34) ----------------

    def copy_selection(self):
        if self._is_text_input_focused():
            return
        tab = self.current_tab()
        if not tab or not tab.selected_ids:
            return
        nodes = [n.to_dict() for n in tab._expand_moving_nodes(tab.selected_ids)]
        node_ids = {n["id"] for n in nodes}
        edges = [
            e.to_dict() for e in tab.model.edges
            if e.id in tab.selected_ids or (e.source_id in node_ids and e.target_id in node_ids)
        ]
        self.clipboard_payload = {
            "source_diagram_type": tab.model.diagram_type,
            "nodes": nodes,
            "edges": edges,
        }
        self.notify(f"{len(nodes)} nó(s) e {len(edges)} aresta(s) copiado(s).", is_error=False)

    def cut_selection(self):
        if self._is_text_input_focused():
            return
        tab = self.current_tab()
        if not tab or not tab.selected_ids:
            return
        self.copy_selection()
        tab.delete_selected()

    def paste_selection(self):
        """RF34: Valida compatibilidade entre diagramas e bloqueia colagem em Diagrama de Sequência."""
        if self._is_text_input_focused():
            return
        tab = self.current_tab()
        if not tab or not self.clipboard_payload:
            return

        if tab.model.diagram_type == "Sequence Diagram":
            self.notify("Colar elementos em diagramas de sequência não é permitido.", is_error=True)
            return

        allowed_nodes = NODE_COMPATIBILITY[tab.model.diagram_type]
        allowed_edges = EDGE_COMPATIBILITY[tab.model.diagram_type]
        for nd in self.clipboard_payload["nodes"]:
            if nd["type"] not in allowed_nodes:
                self.notify(f"Elemento '{nd['type']}' incompatível com {tab.model.diagram_type}.", is_error=True)
                return
        for ed in self.clipboard_payload["edges"]:
            if ed["type"] not in allowed_edges:
                self.notify(f"Aresta '{ed['type']}' incompatível com {tab.model.diagram_type}.", is_error=True)
                return

        id_map = {}
        new_nodes = []
        for nd in self.clipboard_payload["nodes"]:
            new_id = str(uuid.uuid4())
            id_map[nd["id"]] = new_id
            clone = DiagramNode.from_dict(nd)
            clone.id = new_id
            clone.x += 20
            clone.y += 20
            new_nodes.append(clone)

        for clone in new_nodes:
            if clone.parent_id in id_map:
                clone.parent_id = id_map[clone.parent_id]
            else:
                clone.parent_id = None
            tab.model.nodes.append(clone)

        new_edges = []
        for ed in self.clipboard_payload["edges"]:
            if ed["source_id"] in id_map and ed["target_id"] in id_map:
                eclone = DiagramEdge.from_dict(ed)
                eclone.id = str(uuid.uuid4())
                eclone.source_id = id_map[ed["source_id"]]
                eclone.target_id = id_map[ed["target_id"]]
                tab.model.edges.append(eclone)
                new_edges.append(eclone)

        tab.selected_ids = {n.id for n in new_nodes} | {e.id for e in new_edges}
        tab.push_undo_snapshot()
        tab.redraw()

    # ---------------- View Options (RF10, RF37, RF38, RF39, RF40, RF42) ----------------

    def _refresh_all_toolbars(self):
        for tid in self.notebook.tabs():
            self.root.nametowidget(tid).rebuild_toolbar()

    def _redraw_all_tabs(self):
        for tid in self.notebook.tabs():
            self.root.nametowidget(tid).redraw()

    def zoom_in(self):
        if self.zoom_index < len(ZOOM_LEVELS) - 1:
            self.zoom_index += 1
            self._redraw_all_tabs()

    def zoom_out(self):
        if self.zoom_index > 0:
            self.zoom_index -= 1
            self._redraw_all_tabs()

    def reset_zoom(self):
        self.zoom_index = 2
        self._redraw_all_tabs()

    def apply_theme(self):
        """RF40: Alterna entre Modo Claro e Modo Escuro."""
        theme = self.current_theme()
        style = ttk.Style()
        try:
            style.theme_use("clam")
        except Exception:
            pass
        style.configure("TFrame", background=theme["bg"])
        style.configure("TLabel", background=theme["bg"], foreground=theme["fg"])
        style.configure("TNotebook", background=theme["toolbar_bg"])
        style.configure("TNotebook.Tab", background=theme["toolbar_bg"], foreground=theme["fg"], padding=[10, 4])
        style.map("TNotebook.Tab", background=[("selected", theme["panel_bg"])])

        self.root.configure(bg=theme["bg"])
        self.main_stack.configure(bg=theme["bg"])
        if len(self.notebook.tabs()) == 0:
            self._build_welcome_screen()
        self._refresh_all_toolbars()
        self._redraw_all_tabs()

    def open_diagram_size_dialog(self):
        """RF38: Define largura e altura do diagrama (250 a 4000) com Restore Defaults."""
        dlg = tk.Toplevel(self.root)
        dlg.title("Tamanho do Diagrama")
        dlg.transient(self.root)
        dlg.grab_set()
        frm = ttk.Frame(dlg, padding=14)
        frm.pack(fill="both", expand=True)

        ttk.Label(frm, text="Largura (250 - 4000 px):").grid(row=0, column=0, sticky="w", pady=4)
        w_var = tk.IntVar(value=self.diag_width)
        ttk.Spinbox(frm, from_=250, to=4000, textvariable=w_var, width=12).grid(row=0, column=1, pady=4, padx=6)

        ttk.Label(frm, text="Altura (250 - 4000 px):").grid(row=1, column=0, sticky="w", pady=4)
        h_var = tk.IntVar(value=self.diag_height)
        ttk.Spinbox(frm, from_=250, to=4000, textvariable=h_var, width=12).grid(row=1, column=1, pady=4, padx=6)

        def restore():
            w_var.set(self.default_diag_w)
            h_var.set(self.default_diag_h)

        def apply_size():
            w = max(250, min(4000, int(w_var.get())))
            h = max(250, min(4000, int(h_var.get())))
            self.diag_width, self.diag_height = w, h
            tab = self.current_tab()
            if tab:
                tab.model.width = w
                tab.model.height = h
                tab.push_undo_snapshot()
                tab.redraw()
            self.notify(f"Tamanho do diagrama definido para {w}x{h} px.", is_error=False)
            dlg.destroy()

        btns = ttk.Frame(frm)
        btns.grid(row=2, column=0, columnspan=2, pady=(12, 0), sticky="e")
        ttk.Button(btns, text="Restore Defaults", command=restore).pack(side="left", padx=(0, 8))
        ttk.Button(btns, text="OK", command=apply_size).pack(side="right")

    def open_font_dialog(self):
        """RF39: Altera família e tamanho da fonte do diagrama com Restore Default."""
        dlg = tk.Toplevel(self.root)
        dlg.title("Fonte do Diagrama")
        dlg.transient(self.root)
        dlg.grab_set()
        frm = ttk.Frame(dlg, padding=14)
        frm.pack(fill="both", expand=True)

        families = sorted(set(tkfont.families()))
        ttk.Label(frm, text="Fonte:").grid(row=0, column=0, sticky="w", pady=4)
        ff_cb = ttk.Combobox(frm, values=families, width=26)
        ff_cb.set(self.diagram_font_family.get())
        ff_cb.grid(row=0, column=1, pady=4, padx=6)

        ttk.Label(frm, text="Tamanho:").grid(row=1, column=0, sticky="w", pady=4)
        fs_var = tk.IntVar(value=self.diagram_font_size.get())
        ttk.Spinbox(frm, from_=7, to=24, textvariable=fs_var, width=10).grid(row=1, column=1, sticky="w", pady=4, padx=6)

        def restore():
            ff_cb.set("Segoe UI")
            fs_var.set(10)

        def apply_font():
            self.diagram_font_family.set(ff_cb.get() or "Segoe UI")
            self.diagram_font_size.set(max(7, min(24, int(fs_var.get()))))
            self._redraw_all_tabs()
            dlg.destroy()

        btns = ttk.Frame(frm)
        btns.grid(row=2, column=0, columnspan=2, pady=(12, 0), sticky="e")
        ttk.Button(btns, text="Restore Default", command=restore).pack(side="left", padx=(0, 8))
        ttk.Button(btns, text="OK", command=apply_font).pack(side="right")

    def open_notification_duration_dialog(self):
        """RF42: Define duração das notificações de 0 a 10 segundos (padrão 5)."""
        dlg = tk.Toplevel(self.root)
        dlg.title("Duração das Notificações")
        dlg.transient(self.root)
        dlg.grab_set()
        frm = ttk.Frame(dlg, padding=14)
        frm.pack(fill="both", expand=True)

        ttk.Label(frm, text="Duração em segundos (0 = desativado, 0–10):").pack(anchor="w", pady=4)
        dur_var = tk.IntVar(value=self.notification_duration.get())
        ttk.Spinbox(frm, from_=0, to=10, textvariable=dur_var, width=10).pack(anchor="w", pady=4)

        def restore():
            dur_var.set(5)

        def apply_dur():
            val = max(0, min(10, int(dur_var.get())))
            self.notification_duration.set(val)
            dlg.destroy()

        btns = ttk.Frame(frm)
        btns.pack(fill="x", pady=(12, 0))
        ttk.Button(btns, text="Restore Default", command=restore).pack(side="left")
        ttk.Button(btns, text="OK", command=apply_dur).pack(side="right")

    # ---------------- Notificações Temporárias (RF41, RF42) ----------------

    def notify(self, message: str, is_error: bool = False):
        """Exibe toast no canto inferior esquerdo: vermelho para erros, verde para confirmações."""
        dur = self.notification_duration.get()
        if dur <= 0:
            return
        if self.toast_after_id:
            self.root.after_cancel(self.toast_after_id)
            self.toast_after_id = None
        if self.toast_label:
            self.toast_label.destroy()

        bg_color = "#DC2626" if is_error else "#16A34A"
        self.toast_label = tk.Label(
            self.root,
            text=f"  {message}  ",
            bg=bg_color,
            fg="#FFFFFF",
            font=("Segoe UI", 9, "bold"),
            padx=12,
            pady=7,
            relief="flat",
        )
        self.toast_label.place(relx=0.0, rely=1.0, x=16, y=-16, anchor="sw")
        self.toast_after_id = self.root.after(dur * 1000, self._dismiss_toast)

    def _dismiss_toast(self):
        if self.toast_label:
            self.toast_label.destroy()
            self.toast_label = None
        self.toast_after_id = None


def main():
    root = tk.Tk()
    JetUMLApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()