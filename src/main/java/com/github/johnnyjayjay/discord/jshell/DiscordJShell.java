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
        EmbedBuilder embed = new EmbedBuilder().setTitle("JShell").appendDescription("Evaluated your input:                                     ```java\n").appendDescription(input).appendDescription("```");
        Snippet snippet;
        String name, typeName, value;
        boolean unresolved = false;
        boolean rejected = false;
        for (SnippetEvent event : events) {
            String title = null;
            String content;
            StringBuilder out = new StringBuilder();
            snippet = event.snippet();
            content = "\nSource: ```java\n" + (snippet.source().length() < 800 ? snippet.source() : "Source is too long to display") + "```**Type:** ";
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
                    out.append("Created method ").append(name).append(" with signature ").append(methodSnippet.signature());
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
                    content += "error/unknown\n";
            }

            String id = "(" + snippet.id() + ")";
            switch (event.status()) {
                case VALID:
                    switch (event.previousStatus()) {
                        case VALID:
                            title = "Snippet Overwrite - New";
                            break;
                        case NONEXISTENT:
                            title = "Snippet Creation";
                            break;
                        case RECOVERABLE_DEFINED:
                        case RECOVERABLE_NOT_DEFINED:
                            title = "Snippet Change";
                            out.append("\nFixed unresolved references");
                    }
                    break;
                case RECOVERABLE_DEFINED:
                case RECOVERABLE_NOT_DEFINED:
                    title = "Snippet Creation with Unresolved References";
                    unresolved = true;
                    out.append("\nwith unresolved references:");
                    jShell.unresolvedDependencies((DeclarationSnippet) snippet).forEach((dep) -> out.append("\n").append(dep));
                    break;
                case OVERWRITTEN:
                    title = "Snippet Overwrite - Old";
                    out.append("\nThis snippet was overwritten");
                    break;
                case REJECTED:
                    rejected = true;
                    title = "Snippet Rejected";
                    out.append("\nLanguage error:");
                    jShell.diagnostics(snippet).map((diag) -> diag.getMessage(Locale.GERMANY)).forEach((msg) -> out.append("\n").append(msg));
                    break;
            }
            title += " " + id;
            if (event.exception() instanceof EvalException) {
                rejected = true;
                var exception = (EvalException) event.exception();
                String message = exception.getMessage();
                String exceptionClass = exception.getExceptionClassName();
                out.append("\nException thrown\n").append(exceptionClass).append(": ").append(message);
                appendStackTrace(out, exception.getStackTrace());
            } else if (event.exception() instanceof UnresolvedReferenceException) {
                rejected = true;
                var exception = (UnresolvedReferenceException) event.exception();
                String message = exception.getMessage();
                out.append("\nUnresolvedReferenceException: ").append(message);
                appendStackTrace(out, exception.getStackTrace());
            }

            embed.addField(title, content + "Output:```\n" + out.toString() + "```", true);
        }
        if (rejected)
            embed.setColor(Color.RED);
        else if (unresolved)
            embed.setColor(Color.YELLOW);
        else
            embed.setColor(Color.GREEN);
        return embed.build();
    }

    private void appendStackTrace(StringBuilder builder, StackTraceElement[] elements) {
        for (int i = 0; i < elements.length; i++) {
            if (builder.length() > 400) {
                builder.append("\n\t...").append(elements.length - i).append(" more");
                break;
            }
            builder.append("\n\tat: ").append(elements[i]);
        }
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
                ret = "dropped";
                break;
            case OVERWRITTEN:
                ret = "overwritten";
                break;
            case REJECTED:
                ret = "rejected";
                break;
        }
        return ret;
    }

    public void resetJShell() {
        jShell.close();
        jShell = JShell.create();
    }

    public void stopCurrentEval() {
        jShell.stop();
    }

    public Snippet getSnippet(String id) {
        var snippet = jShell.snippets().filter((sn) -> id.equals(sn.id())).findFirst();
        return snippet.orElse(null);
    }

}
