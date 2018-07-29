package com.github.johnnyjayjay.discord.jshell;

import jdk.jshell.*;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class DiscordJShell extends ListenerAdapter implements Evaluator {

    private JShell jShell;

    public DiscordJShell() {
        this.jShell = JShell.create();
    }

    @Override
    public MessageEmbed eval(String input) {
        List<SnippetEvent> events = jShell.eval(input);
        EmbedBuilder embed = new EmbedBuilder().appendDescription("Evaluated your input:```java\n").appendDescription(input).appendDescription("```");
        StringBuilder output = new StringBuilder().append("Evaluated your input.\n");
        Snippet snippet;
        String name;
        String typeName;
        String value;
        for (SnippetEvent event : events) {
            String title = null;
            String content;
            StringBuilder out = new StringBuilder();
            snippet = event.snippet();
            output.append("-------------------------------\n");
            if (event.causeSnippet() != null) {
                title = "Changed Snippet (" + snippet.id() + ")";
                output.append("Changed snippet. Old status: ").append(event.previousStatus().name()).append(" New status: ").append(event.status().name());
            } else {
                switch (event.status()) {
                    case VALID:
                        title = "Snippet Creation (" + snippet.id() + ")";
                        output.append("**New snippet created.**");
                        break;
                    case RECOVERABLE_DEFINED:
                    case RECOVERABLE_NOT_DEFINED:
                        title = "Snippet creation with unresolved dependencies (" + snippet.id() + ")";
                        output.append("**New snippet with unresolved dependencies created.**");
                        break;
                    case OVERWRITTEN:
                        title = "Snippet overwrite (" + snippet.id() + ")";
                        output.append("**Snippet overwritten.**");
                        break;
                    case REJECTED:
                        title = "Snippet rejected (" + snippet.id() + ")";
                        output.append("**Snippet creation rejected.**");
                        break;
                }
            }
            content = "\nSource: ```java\n" + snippet.source() + "```**Type:** ";
            output.append("\nSource: ```java\n").append(snippet.source()).append("```**Type:** ");
            switch (snippet.kind()) {
                case VAR:
                    content += "variable/literal\n";
                    output.append("variable/literal\n");
                    VarSnippet varSnippet = (VarSnippet) snippet;
                    value = event.value();
                    name = varSnippet.name();
                    typeName = varSnippet.typeName();
                    switch (varSnippet.subKind()) {
                        case VAR_DECLARATION_SUBKIND:
                            out.append("Declared variable ").append(typeName).append(" ").append(name).toString();
                            output.append("Declared variable `").append(typeName).append(" ").append(name).append("`.");
                            break;
                        case VAR_DECLARATION_WITH_INITIALIZER_SUBKIND:
                            out.append("Declared variable ").append(typeName).append(" ").append(name).append(" and initialized it with value ").append(value).toString();
                            output.append("Declared variable `").append(typeName).append(" ").append(name)
                                    .append("` and initialized it with value `").append(value).append("`.");
                            break;
                        case TEMP_VAR_EXPRESSION_SUBKIND:
                            out.append(value);
                            output.append("Value of this expression: `").append(value).append("`.");
                    }
                    break;
                case IMPORT:
                    content += "import statement\n";
                    output.append("import statement\n");
                    ImportSnippet importSnippet = (ImportSnippet) snippet;
                    name = importSnippet.name();
                    switch (importSnippet.subKind()) {
                        case STATIC_IMPORT_ON_DEMAND_SUBKIND:
                            out.append("Static on demand import\n");
                            output.append("Static import on demand (.*)");
                            break;
                        case SINGLE_STATIC_IMPORT_SUBKIND:
                            out.append("Static import\n");
                            output.append("Static import");
                            break;
                        case TYPE_IMPORT_ON_DEMAND_SUBKIND:
                            out.append("On demand import\n");
                            output.append("Import on demand (.*)");
                            break;
                        case SINGLE_TYPE_IMPORT_SUBKIND:
                            out.append("Import\n");
                            output.append("Import");
                    }
                    out.append(name);
                    output.append(": `").append(name).append("`");
                    break;
                case TYPE_DECL:
                    content += "type declaration\n";
                    output.append("type declaration\n");
                    TypeDeclSnippet typeDeclSnippet = (TypeDeclSnippet) snippet;
                    name = typeDeclSnippet.name();
                    output.append("Created ");
                    out.append("Created ");
                    switch (typeDeclSnippet.subKind()) {
                        case CLASS_SUBKIND:
                            out.append("class");
                            output.append("class");
                            break;
                        case INTERFACE_SUBKIND:
                            out.append("interface");
                            output.append("interface");
                            break;
                        case ENUM_SUBKIND:
                            out.append("enum");
                            output.append("enum");
                            break;
                        case ANNOTATION_TYPE_SUBKIND:
                            out.append("annotation");
                            output.append("annotation");
                    }
                    out.append(" ").append(name);
                    output.append(" `").append(name).append("`.");
                    break;
                case STATEMENT:
                    content += "statement\n";
                    out.append("Executed statement");
                    output.append("statement\n").append("Success.");
                    break;
                case METHOD:
                    content += "method\n";
                    output.append("method\n");
                    MethodSnippet methodSnippet = (MethodSnippet) snippet;
                    name = methodSnippet.name();
                    out.append("Created method ").append(name).append(" with signature ").append(methodSnippet.signature()).toString();
                    output.append("Method signature: `").append(methodSnippet.signature()).append("`\n");
                    break;
                case EXPRESSION:
                    content += "expression\n";
                    output.append("expression\n");
                    ExpressionSnippet expressionSnippet = (ExpressionSnippet) snippet;
                    value = event.value();
                    switch (expressionSnippet.subKind()) {
                        case OTHER_EXPRESSION_SUBKIND:
                        case VAR_VALUE_SUBKIND:
                            out.append(value);
                            output.append("Evaluated expression: `").append(value).append("`");
                            break;
                        case ASSIGNMENT_SUBKIND:
                            typeName = expressionSnippet.typeName();
                            name = expressionSnippet.name();
                            out.append("Assigned value ").append(value).append(" to variable ").append(typeName).append(" ").append(name).toString();
                            output.append("Assigned value `").append(value).append("` to variable `").append(typeName)
                                    .append(" ").append(name).append("`.");
                    }
                    break;
                case ERRONEOUS:
                    content += "error/unknown";
                    output.append("unknown. Error cause:\n```\n");
                    jShell.diagnostics(snippet).map((diag) -> diag.getMessage(Locale.GERMANY)).forEach(out::append);
                    output.append("```");
            }
            embed.addField(title, content + "Output:```\n" + out.toString() + "```", true);
            output.append("\nSnippet id: `").append(snippet.id()).append("`\n\n");
        }
        return embed.setTitle("JShell").setColor(Color.MAGENTA).build();
    }

    public Stream<MethodSnippet> getMethods() {
        return jShell.methods();
    }

    public Stream<VarSnippet> getVariables() {
        return jShell.variables();
    }

    public Stream<ImportSnippet> getImports() {
        return jShell.imports();
    }

    public Stream<TypeDeclSnippet> getTypes() {
        return jShell.types();
    }

    public boolean drop(String id) {
        boolean success = false;
        var snippet = jShell.snippets().filter((sn) -> id.equals(sn.id())).findFirst();
        if (snippet.isPresent()) {
            jShell.drop(snippet.get());
            success = true;
        }
        return success;
    }

    public String getStatus(Snippet snippet) {
        String ret = "unknown";
        switch (jShell.status(snippet)) {
            case RECOVERABLE_DEFINED:
            case RECOVERABLE_NOT_DEFINED:
                ret = "with missing dependencies";
                break;
            case VALID:
                ret = "valid and active";
                break;
            case DROPPED:
                ret = "dropped / not usable";
                break;
            case OVERWRITTEN:
                ret = "overwritten";
                break;
            case REJECTED:
                ret = "rejected / not usable";
                break;
        }
        return ret;
    }

    public void resetJShell() {
        jShell.close();
        jShell = JShell.create();
    }

    public Snippet getSnippet(String id) {
        var snippet = jShell.snippets().filter((sn) -> id.equals(sn.id())).findFirst();
        return snippet.orElse(null);
    }

}
