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
        Snippet snippet;
        String name;
        String typeName;
        String value;
        for (SnippetEvent event : events) {
            String title = null;
            String content;
            StringBuilder out = new StringBuilder();
            snippet = event.snippet();
            if (event.causeSnippet() != null) {
                title = "Changed Snippet (" + snippet.id() + ")";
            } else {
                switch (event.status()) {
                    case VALID:
                        title = "Snippet Creation (" + snippet.id() + ")";
                        break;
                    case RECOVERABLE_DEFINED:
                    case RECOVERABLE_NOT_DEFINED:
                        title = "Snippet creation with unresolved dependencies (" + snippet.id() + ")";
                        break;
                    case OVERWRITTEN:
                        title = "Snippet overwrite (" + snippet.id() + ")";
                        break;
                    case REJECTED:
                        title = "Snippet rejected (" + snippet.id() + ")";
                        break;
                }
            }
            content = "\nSource: ```java\n" + snippet.source() + "```**Type:** ";
            switch (snippet.kind()) {
                case VAR:
                    content += "variable/literal\n";
                    VarSnippet varSnippet = (VarSnippet) snippet;
                    value = event.value();
                    name = varSnippet.name();
                    typeName = varSnippet.typeName();
                    switch (varSnippet.subKind()) {
                        case VAR_DECLARATION_SUBKIND:
                            out.append("Declared variable ").append(typeName).append(" ").append(name);
                            break;
                        case VAR_DECLARATION_WITH_INITIALIZER_SUBKIND:
                            out.append("Declared variable ").append(typeName).append(" ").append(name).append(" and initialized it with value ").append(value).toString();
                            break;
                        case TEMP_VAR_EXPRESSION_SUBKIND:
                            out.append(value);
                    }
                    break;
                case IMPORT:
                    content += "import statement\n";
                    ImportSnippet importSnippet = (ImportSnippet) snippet;
                    name = importSnippet.name();
                    switch (importSnippet.subKind()) {
                        case STATIC_IMPORT_ON_DEMAND_SUBKIND:
                            out.append("Static on demand import\n");
                            break;
                        case SINGLE_STATIC_IMPORT_SUBKIND:
                            out.append("Static import\n");
                            break;
                        case TYPE_IMPORT_ON_DEMAND_SUBKIND:
                            out.append("On demand import\n");
                            break;
                        case SINGLE_TYPE_IMPORT_SUBKIND:
                            out.append("Import\n");
                    }
                    out.append(name);
                    break;
                case TYPE_DECL:
                    content += "type declaration\n";
                    TypeDeclSnippet typeDeclSnippet = (TypeDeclSnippet) snippet;
                    name = typeDeclSnippet.name();
                    out.append("Created ");
                    switch (typeDeclSnippet.subKind()) {
                        case CLASS_SUBKIND:
                            out.append("class");
                            break;
                        case INTERFACE_SUBKIND:
                            out.append("interface");
                            break;
                        case ENUM_SUBKIND:
                            out.append("enum");
                            break;
                        case ANNOTATION_TYPE_SUBKIND:
                            out.append("annotation");
                    }
                    out.append(" ").append(name);
                    break;
                case STATEMENT:
                    content += "statement\n";
                    out.append("Executed statement");
                    break;
                case METHOD:
                    content += "method\n";
                    MethodSnippet methodSnippet = (MethodSnippet) snippet;
                    name = methodSnippet.name();
                    out.append("Created method ").append(name).append(" with signature ").append(methodSnippet.signature()).toString();
                    break;
                case EXPRESSION:
                    content += "expression\n";
                    ExpressionSnippet expressionSnippet = (ExpressionSnippet) snippet;
                    value = event.value();
                    switch (expressionSnippet.subKind()) {
                        case OTHER_EXPRESSION_SUBKIND:
                        case VAR_VALUE_SUBKIND:
                            out.append(value);
                            break;
                        case ASSIGNMENT_SUBKIND:
                            typeName = expressionSnippet.typeName();
                            name = expressionSnippet.name();
                            out.append("Assigned value ").append(value).append(" to variable ").append(typeName).append(" ").append(name).toString();
                    }
                    break;
                case ERRONEOUS:
                    content += "error/unknown";
                    jShell.diagnostics(snippet).map((diag) -> diag.getMessage(Locale.GERMANY)).forEach(out::append);
            }
            embed.addField(title, content + "Output:```\n" + out.toString() + "```", true);
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
