package com.samco.trackandgraph.antlr.generated;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TnG2Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TnG2Visitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TnG2Parser#datatransformationFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatransformationFunction(TnG2Parser.DatatransformationFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TnG2Parser#line}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLine(TnG2Parser.LineContext ctx);
	/**
	 * Visit a parse tree produced by the {@code varDeclarationStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDeclarationStatement(TnG2Parser.VarDeclarationStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assignmentStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentStatement(TnG2Parser.AssignmentStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code printStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrintStatement(TnG2Parser.PrintStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link TnG2Parser#argumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentList(TnG2Parser.ArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link TnG2Parser#print}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint(TnG2Parser.PrintContext ctx);
	/**
	 * Visit a parse tree produced by {@link TnG2Parser#varDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDeclaration(TnG2Parser.VarDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link TnG2Parser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(TnG2Parser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimalLiteral(TnG2Parser.DecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code minusExpression}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinusExpression(TnG2Parser.MinusExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(TnG2Parser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(TnG2Parser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntLiteral(TnG2Parser.IntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code timePeriod}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimePeriod(TnG2Parser.TimePeriodContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenExpression}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenExpression(TnG2Parser.ParenExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code binaryOperation}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOperation(TnG2Parser.BinaryOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aggregationFunction}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregationFunction(TnG2Parser.AggregationFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code varReference}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarReference(TnG2Parser.VarReferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Second}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecond(TnG2Parser.SecondContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Minute}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinute(TnG2Parser.MinuteContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Hourly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHourly(TnG2Parser.HourlyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Daily}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDaily(TnG2Parser.DailyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Weekly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWeekly(TnG2Parser.WeeklyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Monthly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMonthly(TnG2Parser.MonthlyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Yearly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYearly(TnG2Parser.YearlyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Average}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAverage(TnG2Parser.AverageContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Median}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMedian(TnG2Parser.MedianContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Min}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMin(TnG2Parser.MinContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Max}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMax(TnG2Parser.MaxContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Earliest}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEarliest(TnG2Parser.EarliestContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Latest}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLatest(TnG2Parser.LatestContext ctx);
}