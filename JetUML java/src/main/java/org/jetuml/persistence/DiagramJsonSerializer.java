package org.jetuml.persistence;

import com.google.gson.*;
import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.*;
import org.jetuml.model.edges.*;
import org.jetuml.model.nodes.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Serializador e desserializador em formato JSON aberto, legivel e documentado (RF02, RF38, RNF02).
 * <p>
 * Estrutura do documento JSON:
 * {
 * "formatVersion": "1.0",
 * "diagramType": "CLASS | SEQUENCE | STATE | OBJECT | USE_CASE",
 * "width": 1100,
 * "height": 750,
 * "nodes": [ { "id": "...", "type": "...", "x": 100, "y": 80, "width": 130, "height": 80, "properties": {...}, "children": [...] } ],
 * "edges": [ { "id": "...", "type": "...", "startNodeId": "...", "endNodeId": "...", "properties": {...} } ]
 * }
 */
public final class DiagramJsonSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private DiagramJsonSerializer() {
    }

    public static void saveToFile(Diagram diagram, Path path) throws IOException {
        String json = toJson(diagram);
        Files.writeString(path, json, StandardCharsets.UTF_8);
        diagram.setFilePath(path);
        diagram.setDirty(false);
    }

    public static Diagram loadFromFile(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Diagram diagram = fromJson(json);
        diagram.setFilePath(path);
        diagram.ensureFitsContent(); // RF38: Aumenta automaticamente se o arquivo for maior que a area configurada
        diagram.setDirty(false);
        return diagram;
    }

    public static String toJson(Diagram diagram) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", "1.0");
        root.addProperty("diagramType", diagram.getType().name());
        root.addProperty("width", diagram.getWidth());
        root.addProperty("height", diagram.getHeight());

        JsonArray nodesArray = new JsonArray();
        for (Node rootNode : diagram.getRootNodes()) {
            nodesArray.add(serializeNode(rootNode));
        }
        root.add("nodes", nodesArray);

        JsonArray edgesArray = new JsonArray();
        for (Edge edge : diagram.getEdges()) {
            edgesArray.add(serializeEdge(edge));
        }
        root.add("edges", edgesArray);

        return GSON.toJson(root);
    }

    private static JsonObject serializeNode(Node node) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", node.getId());
        obj.addProperty("type", node.getElementType());
        obj.addProperty("x", node.getPosition().x());
        obj.addProperty("y", node.getPosition().y());
        obj.addProperty("width", node.getSize().width());
        obj.addProperty("height", node.getSize().height());

        JsonObject props = new JsonObject();
        if (node instanceof ClassNode c) {
            props.addProperty("name", c.getName());
            props.addProperty("attributes", c.getAttributes());
            props.addProperty("methods", c.getMethods());
        } else if (node instanceof InterfaceNode i) {
            props.addProperty("name", i.getName());
            props.addProperty("methods", i.getMethods());
        } else if (node instanceof PackageNode p) {
            props.addProperty("name", p.getName());
        } else if (node instanceof PackageDescriptionNode pd) {
            props.addProperty("name", pd.getName());
            props.addProperty("contents", pd.getContents());
        } else if (node instanceof ImplicitParameterNode ip) {
            props.addProperty("name", ip.getName());
            props.addProperty("topOffsetY", ip.getTopOffsetY());
        } else if (node instanceof CallNode call) {
            props.addProperty("openBottom", call.isOpenBottom());
        } else if (node instanceof StateNode st) {
            props.addProperty("name", st.getName());
        } else if (node instanceof ObjectNode o) {
            props.addProperty("name", o.getName());
        } else if (node instanceof FieldNode f) {
            props.addProperty("name", f.getName());
            props.addProperty("value", f.getValue());
        } else if (node instanceof ActorNode a) {
            props.addProperty("name", a.getName());
        } else if (node instanceof UseCaseNode uc) {
            props.addProperty("name", uc.getName());
        } else if (node instanceof NoteNode note) {
            props.addProperty("text", note.getText());
        }
        obj.add("properties", props);

        if (node instanceof ParentNode parent && !parent.getChildren().isEmpty()) {
            JsonArray childrenArr = new JsonArray();
            for (ChildNode child : parent.getChildren()) {
                childrenArr.add(serializeNode(child));
            }
            obj.add("children", childrenArr);
        }
        return obj;
    }

    private static JsonObject serializeEdge(Edge edge) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", edge.getId());
        obj.addProperty("type", edge.getElementType());
        obj.addProperty("startNodeId", edge.getStartNode().getId());
        obj.addProperty("endNodeId", edge.getEndNode().getId());

        JsonObject props = new JsonObject();
        if (edge instanceof ClassRelationshipEdge rel) {
            props.addProperty("kind", rel.getKind().name());
            props.addProperty("directionality", rel.getDirectionality().name());
            props.addProperty("startLabel", rel.getStartLabel());
            props.addProperty("middleLabel", rel.getMiddleLabel());
            props.addProperty("endLabel", rel.getEndLabel());
        } else if (edge instanceof CallEdge call) {
            props.addProperty("middleLabel", call.getMiddleLabel());
            props.addProperty("createCall", call.isCreateCall());
        } else if (edge instanceof ReturnEdge ret) {
            props.addProperty("middleLabel", ret.getMiddleLabel());
        } else if (edge instanceof TransitionEdge tr) {
            props.addProperty("middleLabel", tr.getMiddleLabel());
        } else if (edge instanceof CollaborationEdge col) {
            props.addProperty("middleLabel", col.getMiddleLabel());
        } else if (edge instanceof UseCaseEdge uc) {
            props.addProperty("kind", uc.getKind().name());
            props.addProperty("dependencyType", uc.getDependencyType().name());
        }
        obj.add("properties", props);
        return obj;
    }

    public static Diagram fromJson(String json) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String typeStr = root.get("diagramType").getAsString();
            DiagramType diagramType = DiagramType.valueOf(typeStr);
            Diagram diagram = new Diagram(diagramType);

            if (root.has("width") && root.has("height")) {
                diagram.setCanvasSize(root.get("width").getAsInt(), root.get("height").getAsInt());
            }

            Map<String, Node> nodeRegistry = new LinkedHashMap<>();
            JsonArray nodesArr = root.getAsJsonArray("nodes");
            if (nodesArr != null) {
                for (JsonElement elem : nodesArr) {
                    Node rootNode = deserializeNode(elem.getAsJsonObject(), nodeRegistry);
                    if (rootNode != null) {
                        diagram.addRootNode(rootNode);
                    }
                }
            }

            // Reajustar layout de CallNodes em Sequence Diagrams
            for (Node node : diagram.getRootNodes()) {
                if (node instanceof ImplicitParameterNode ip) {
                    ip.layoutChildren();
                }
            }

            JsonArray edgesArr = root.getAsJsonArray("edges");
            if (edgesArr != null) {
                for (JsonElement elem : edgesArr) {
                    Edge edge = deserializeEdge(elem.getAsJsonObject(), nodeRegistry);
                    if (edge != null) {
                        diagram.addEdge(edge);
                    }
                }
            }

            EdgeRouter.routeAll(diagram);
            diagram.ensureFitsContent();
            diagram.setDirty(false);
            return diagram;
        } catch (Exception ex) {
            throw new IOException("Falha ao ler arquivo JSON do diagrama: " + ex.getMessage(), ex);
        }
    }

    private static Node deserializeNode(JsonObject obj, Map<String, Node> registry) {
        String type = obj.get("type").getAsString();
        Optional<Node> createdOpt = ElementFactory.createNodeByTypeName(type);
        if (createdOpt.isEmpty()) return null;

        Node node = createdOpt.get();
        if (obj.has("id")) {
            node.setId(obj.get("id").getAsString());
        }
        int x = obj.has("x") ? obj.get("x").getAsInt() : 0;
        int y = obj.has("y") ? obj.get("y").getAsInt() : 0;
        node.setPosition(new Point2DInt(x, y));

        JsonObject props = obj.has("properties") ? obj.getAsJsonObject("properties") : new JsonObject();
        applyNodeProperties(node, props);

        if (obj.has("width") && obj.has("height")) {
            node.setSize(new Dimension2DInt(obj.get("width").getAsInt(), obj.get("height").getAsInt()));
        }

        registry.put(node.getId(), node);

        if (node instanceof ParentNode parent && obj.has("children")) {
            JsonArray childrenArr = obj.getAsJsonArray("children");
            for (JsonElement childElem : childrenArr) {
                Node childNode = deserializeNode(childElem.getAsJsonObject(), registry);
                if (childNode instanceof ChildNode child) {
                    parent.addChild(child);
                }
            }
        }
        return node;
    }

    private static void applyNodeProperties(Node node, JsonObject props) {
        if (node instanceof ClassNode c) {
            if (props.has("name")) c.setName(props.get("name").getAsString());
            if (props.has("attributes")) c.setAttributes(props.get("attributes").getAsString());
            if (props.has("methods")) c.setMethods(props.get("methods").getAsString());
        } else if (node instanceof InterfaceNode i) {
            if (props.has("name")) i.setName(props.get("name").getAsString());
            if (props.has("methods")) i.setMethods(props.get("methods").getAsString());
        } else if (node instanceof PackageNode p) {
            if (props.has("name")) p.setName(props.get("name").getAsString());
        } else if (node instanceof PackageDescriptionNode pd) {
            if (props.has("name")) pd.setName(props.get("name").getAsString());
            if (props.has("contents")) pd.setContents(props.get("contents").getAsString());
        } else if (node instanceof ImplicitParameterNode ip) {
            if (props.has("name")) ip.setName(props.get("name").getAsString());
            if (props.has("topOffsetY")) ip.setTopOffsetY(props.get("topOffsetY").getAsInt());
        } else if (node instanceof CallNode call) {
            if (props.has("openBottom")) call.setOpenBottom(props.get("openBottom").getAsBoolean());
        } else if (node instanceof StateNode st) {
            if (props.has("name")) st.setName(props.get("name").getAsString());
        } else if (node instanceof ObjectNode o) {
            if (props.has("name")) o.setName(props.get("name").getAsString());
        } else if (node instanceof FieldNode f) {
            if (props.has("name")) f.setName(props.get("name").getAsString());
            if (props.has("value")) f.setValue(props.get("value").getAsString());
        } else if (node instanceof ActorNode a) {
            if (props.has("name")) a.setName(props.get("name").getAsString());
        } else if (node instanceof UseCaseNode uc) {
            if (props.has("name")) uc.setName(props.get("name").getAsString());
        } else if (node instanceof NoteNode note) {
            if (props.has("text")) note.setText(props.get("text").getAsString());
        }
    }

    private static Edge deserializeEdge(JsonObject obj, Map<String, Node> registry) {
        String type = obj.get("type").getAsString();
        String startId = obj.get("startNodeId").getAsString();
        String endId = obj.get("endNodeId").getAsString();
        Node start = registry.get(startId);
        Node end = registry.get(endId);
        if (start == null || end == null) return null;

        JsonObject props = obj.has("properties") ? obj.getAsJsonObject("properties") : new JsonObject();
        Edge edge = switch (type) {
            case ClassRelationshipEdge.ELEMENT_TYPE -> {
                ClassRelationshipEdge.RelationshipKind kind = ClassRelationshipEdge.RelationshipKind.valueOf(
                        props.get("kind").getAsString()
                );
                ClassRelationshipEdge rel = new ClassRelationshipEdge(kind, start, end);
                if (props.has("directionality")) {
                    rel.setDirectionality(Directionality.valueOf(props.get("directionality").getAsString()));
                }
                if (props.has("startLabel")) rel.setStartLabel(props.get("startLabel").getAsString());
                if (props.has("middleLabel")) rel.setMiddleLabel(props.get("middleLabel").getAsString());
                if (props.has("endLabel")) rel.setEndLabel(props.get("endLabel").getAsString());
                yield rel;
            }
            case CallEdge.ELEMENT_TYPE -> {
                boolean createCall = props.has("createCall") && props.get("createCall").getAsBoolean();
                CallEdge call = new CallEdge(start, end, createCall);
                if (props.has("middleLabel")) call.setMiddleLabel(props.get("middleLabel").getAsString());
                if (start instanceof CallNode caller && end instanceof CallNode callee) {
                    caller.addChild(callee);
                }
                yield call;
            }
            case ReturnEdge.ELEMENT_TYPE -> {
                ReturnEdge ret = new ReturnEdge(start, end);
                if (props.has("middleLabel")) ret.setMiddleLabel(props.get("middleLabel").getAsString());
                yield ret;
            }
            case TransitionEdge.ELEMENT_TYPE -> {
                TransitionEdge tr = new TransitionEdge(start, end);
                if (props.has("middleLabel")) tr.setMiddleLabel(props.get("middleLabel").getAsString());
                yield tr;
            }
            case ReferenceEdge.ELEMENT_TYPE -> new ReferenceEdge(start, end);
            case CollaborationEdge.ELEMENT_TYPE -> {
                CollaborationEdge col = new CollaborationEdge(start, end);
                if (props.has("middleLabel")) col.setMiddleLabel(props.get("middleLabel").getAsString());
                yield col;
            }
            case UseCaseEdge.ELEMENT_TYPE -> {
                UseCaseEdge.UseCaseKind kind = UseCaseEdge.UseCaseKind.valueOf(props.get("kind").getAsString());
                UseCaseEdge uc = new UseCaseEdge(kind, start, end);
                if (props.has("dependencyType")) {
                    uc.setDependencyType(UseCaseEdge.DependencyType.valueOf(props.get("dependencyType").getAsString()));
                }
                yield uc;
            }
            case NoteConnectorEdge.ELEMENT_TYPE -> new NoteConnectorEdge(start, end);
            default -> null;
        };

        if (edge != null && obj.has("id")) {
            edge.setId(obj.get("id").getAsString());
        }
        return edge;
    }
}
