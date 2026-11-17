# JetUML — reimplementação acadêmica (Java 21 + Maven + JavaFX)

Projeto independente desenvolvido a partir dos requisitos do Anexo II de `DescriçãoProjeto.pdf` e do guia de usuário
fornecido. Não utiliza nem reproduz o código-fonte do JetUML original.

## Requisitos de ambiente

- JDK 21
- Maven 3.9+
- JavaFX 21 (resolvido pelo Maven)

## Executar

```bash
mvn clean javafx:run
```

## Testar

```bash
mvn test
```

## Estrutura

- `model`: modelo de domínio de diagramas, nós e arestas.
- `validation`: validações estruturais iniciais.
- `command`: comandos e histórico Undo/Redo.
- `persistence`: leitura e gravação JSON.
- `ui`: interface JavaFX, canvas, ferramentas e propriedades.
- `app`: inicialização, menus, abas e integração.

## Estado de conformidade

Esta é uma primeira implementação executável/prototípica, não uma declaração de conformidade integral. A matriz
`REQUIREMENTS.md` identifica o que está implementado e o que ainda precisa ser concluído/testado. Em particular, ainda
faltam regras completas de sequência, pacote com Shift, seleção múltipla/laço, copiar/colar/recortar de elementos, SVG,
tipografia configurável, roteamento avançado de arestas e diversas propriedades específicas do Anexo II.

## Formato JSON

O formato inicial é JSON aberto e legível, com `type`, `nodes`, `edges`, IDs, coordenadas, propriedades e relações por
ID. A estrutura ainda é uma versão inicial e deverá ser versionada antes de ser considerada estável.
