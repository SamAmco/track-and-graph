package com.samco.trackandgraph.antlr.generated;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TnG2Parser}.
 */
public interface TnG2Listener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TnG2Parser#datatransformationFunction}.
	 * @param ctx the parse tree
	 */
	void enterDatatransformationFunction(TnG2Parser.DatatransformationFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TnG2Parser#datatransformationFunction}.
	 * @param ctx the parse tree
	 */
	void exitDatatransformationFunction(TnG2Parser.DatatransformationFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TnG2Parser#line}.
	 * @param ctx the parse tree
	 */
	void enterLine(TnG2Parser.LineContext ctx);
	/**
	 * Exit a parse tree produced by {@link TnG2Parser#line}.
	 * @param ctx the parse tree
	 */
	void exitLine(TnG2Parser.LineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code varDeclarationStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 */
	void enterVarDeclarationStatement(TnG2Parser.VarDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code varDeclarationStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 */
	void exitVarDeclarationStatement(TnG2Parser.VarDeclarationStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code assignmentStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentStatement(TnG2Parser.AssignmentStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code assignmentStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentStatement(TnG2Parser.AssignmentStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code printStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 */
	void enterPrintStatement(TnG2Parser.PrintStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code printStatement}
	 * labeled alternative in {@link TnG2Parser#statement}.
	 * @param ctx the parse tree
	 */
	void exitPrintStatement(TnG2Parser.PrintStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link TnG2Parser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(TnG2Parser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TnG2Parser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(TnG2Parser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link TnG2Parser#print}.
	 * @param ctx the parse tree
	 */
	void enterPrint(TnG2Parser.PrintContext ctx);
	/**
	 * Exit a parse tree produced by {@link TnG2Parser#print}.
	 * @param ctx the parse tree
	 */
	void exitPrint(TnG2Parser.PrintContext ctx);
	/**
	 * Enter a parse tree produced by {@link TnG2Parser#varDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVarDeclaration(TnG2Parser.VarDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TnG2Parser#varDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVarDeclaration(TnG2Parser.VarDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TnG2Parser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(TnG2Parser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TnG2Parser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(TnG2Parser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(TnG2Parser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(TnG2Parser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code minusExpression}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterMinusExpression(TnG2Parser.MinusExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code minusExpression}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitMinusExpression(TnG2Parser.MinusExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(TnG2Parser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(TnG2Parser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(TnG2Parser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(TnG2Parser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterIntLiteral(TnG2Parser.IntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intLiteral}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitIntLiteral(TnG2Parser.IntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timePeriod}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterTimePeriod(TnG2Parser.TimePeriodContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timePeriod}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitTimePeriod(TnG2Parser.TimePeriodContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenExpression}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterParenExpression(TnG2Parser.ParenExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenExpression}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitParenExpression(TnG2Parser.ParenExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryOperation}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryOperation(TnG2Parser.BinaryOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryOperation}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryOperation(TnG2Parser.BinaryOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aggregationFunction}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAggregationFunction(TnG2Parser.AggregationFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aggregationFunction}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAggregationFunction(TnG2Parser.AggregationFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code varReference}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterVarReference(TnG2Parser.VarReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code varReference}
	 * labeled alternative in {@link TnG2Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitVarReference(TnG2Parser.VarReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Second}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void enterSecond(TnG2Parser.SecondContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Second}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void exitSecond(TnG2Parser.SecondContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Minute}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void enterMinute(TnG2Parser.MinuteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Minute}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void exitMinute(TnG2Parser.MinuteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Hourly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void enterHourly(TnG2Parser.HourlyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Hourly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void exitHourly(TnG2Parser.HourlyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Daily}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void enterDaily(TnG2Parser.DailyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Daily}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void exitDaily(TnG2Parser.DailyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Weekly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void enterWeekly(TnG2Parser.WeeklyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Weekly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void exitWeekly(TnG2Parser.WeeklyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Monthly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void enterMonthly(TnG2Parser.MonthlyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Monthly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void exitMonthly(TnG2Parser.MonthlyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Yearly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void enterYearly(TnG2Parser.YearlyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Yearly}
	 * labeled alternative in {@link TnG2Parser#time_period}.
	 * @param ctx the parse tree
	 */
	void exitYearly(TnG2Parser.YearlyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Average}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void enterAverage(TnG2Parser.AverageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Average}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void exitAverage(TnG2Parser.AverageContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Median}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void enterMedian(TnG2Parser.MedianContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Median}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void exitMedian(TnG2Parser.MedianContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Min}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void enterMin(TnG2Parser.MinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Min}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void exitMin(TnG2Parser.MinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Max}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void enterMax(TnG2Parser.MaxContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Max}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void exitMax(TnG2Parser.MaxContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Earliest}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void enterEarliest(TnG2Parser.EarliestContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Earliest}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void exitEarliest(TnG2Parser.EarliestContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Latest}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void enterLatest(TnG2Parser.LatestContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Latest}
	 * labeled alternative in {@link TnG2Parser#aggregation_function}.
	 * @param ctx the parse tree
	 */
	void exitLatest(TnG2Parser.LatestContext ctx);
}