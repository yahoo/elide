// Generated from C:/Users/KiangTeng/elide/elide-core/src/main/antlr4/com/yahoo/elide/generated/parsers\Expression.g4 by ANTLR 4.5.1
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ExpressionParser}.
 */
public interface ExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(ExpressionParser.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(ExpressionParser.StartContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NOT}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNOT(ExpressionParser.NOTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NOT}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNOT(ExpressionParser.NOTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OR}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterOR(ExpressionParser.ORContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OR}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitOR(ExpressionParser.ORContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AND}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAND(ExpressionParser.ANDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AND}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAND(ExpressionParser.ANDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PAREN}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPAREN(ExpressionParser.PARENContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PAREN}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPAREN(ExpressionParser.PARENContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EXPRESSION}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterEXPRESSION(ExpressionParser.EXPRESSIONContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EXPRESSION}
	 * labeled alternative in {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitEXPRESSION(ExpressionParser.EXPRESSIONContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#expressionClass}.
	 * @param ctx the parse tree
	 */
	void enterExpressionClass(ExpressionParser.ExpressionClassContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#expressionClass}.
	 * @param ctx the parse tree
	 */
	void exitExpressionClass(ExpressionParser.ExpressionClassContext ctx);
}