// Generated from C:/Users/KiangTeng/elide/elide-core/src/main/antlr4/com/yahoo/elide/generated/parsers\Expression.g4 by ANTLR 4.5.1
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ExpressionParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ExpressionVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart(ExpressionParser.StartContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NOT}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNOT(ExpressionParser.NOTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OR}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOR(ExpressionParser.ORContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AND}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAND(ExpressionParser.ANDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PAREN}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPAREN(ExpressionParser.PARENContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EXPRESSION}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEXPRESSION(ExpressionParser.EXPRESSIONContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#expressionClass}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionClass(ExpressionParser.ExpressionClassContext ctx);
}