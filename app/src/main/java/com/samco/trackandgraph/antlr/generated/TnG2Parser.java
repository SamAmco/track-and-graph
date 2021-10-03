package com.samco.trackandgraph.antlr.generated;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TnG2Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		NEWLINE=1, WS=2, VAR=3, PRINT=4, AS=5, INT=6, DECIMAL=7, SECOND=8, MINUTE=9, 
		HOURLY=10, DAILY=11, WEEKLY=12, MONTHLY=13, YEARLY=14, AVERAGE=15, MEDIAN=16, 
		MIN=17, MAX=18, EARLIEST=19, LATEST=20, INTLIT=21, DECLIT=22, PLUS=23, 
		MINUS=24, ASTERISK=25, DIVISION=26, ASSIGN=27, LPAREN=28, RPAREN=29, FUNCTION_NAME=30, 
		ID=31, STRING=32, COMMA=33;
	public static final int
		RULE_datatransformationFunction = 0, RULE_line = 1, RULE_statement = 2, 
		RULE_argumentList = 3, RULE_print = 4, RULE_varDeclaration = 5, RULE_assignment = 6, 
		RULE_expression = 7, RULE_time_period = 8, RULE_aggregation_function = 9;
	private static String[] makeRuleNames() {
		return new String[] {
			"datatransformationFunction", "line", "statement", "argumentList", "print", 
			"varDeclaration", "assignment", "expression", "time_period", "aggregation_function"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'var'", "'print'", "'as'", "'Int'", "'Decimal'", "'SECOND'", 
			"'MINUTE'", null, null, null, null, null, "'AVERAGE'", "'MEDIAN'", "'MIN'", 
			"'MAX'", "'EARLIEST'", "'LATEST'", null, null, "'+'", "'-'", "'*'", "'/'", 
			"'='", "'('", "')'", null, null, null, "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "NEWLINE", "WS", "VAR", "PRINT", "AS", "INT", "DECIMAL", "SECOND", 
			"MINUTE", "HOURLY", "DAILY", "WEEKLY", "MONTHLY", "YEARLY", "AVERAGE", 
			"MEDIAN", "MIN", "MAX", "EARLIEST", "LATEST", "INTLIT", "DECLIT", "PLUS", 
			"MINUS", "ASTERISK", "DIVISION", "ASSIGN", "LPAREN", "RPAREN", "FUNCTION_NAME", 
			"ID", "STRING", "COMMA"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "TnG2.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TnG2Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class DatatransformationFunctionContext extends ParserRuleContext {
		public LineContext lines;
		public List<LineContext> line() {
			return getRuleContexts(LineContext.class);
		}
		public LineContext line(int i) {
			return getRuleContext(LineContext.class,i);
		}
		public DatatransformationFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_datatransformationFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterDatatransformationFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitDatatransformationFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitDatatransformationFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DatatransformationFunctionContext datatransformationFunction() throws RecognitionException {
		DatatransformationFunctionContext _localctx = new DatatransformationFunctionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_datatransformationFunction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(21); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(20);
				((DatatransformationFunctionContext)_localctx).lines = line();
				}
				}
				setState(23); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << VAR) | (1L << PRINT) | (1L << ID))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LineContext extends ParserRuleContext {
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode NEWLINE() { return getToken(TnG2Parser.NEWLINE, 0); }
		public TerminalNode EOF() { return getToken(TnG2Parser.EOF, 0); }
		public LineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_line; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitLine(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LineContext line() throws RecognitionException {
		LineContext _localctx = new LineContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_line);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(25);
			statement();
			setState(26);
			_la = _input.LA(1);
			if ( !(_la==EOF || _la==NEWLINE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
	 
		public StatementContext() { }
		public void copyFrom(StatementContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class PrintStatementContext extends StatementContext {
		public PrintContext print() {
			return getRuleContext(PrintContext.class,0);
		}
		public PrintStatementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterPrintStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitPrintStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitPrintStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AssignmentStatementContext extends StatementContext {
		public AssignmentContext assignment() {
			return getRuleContext(AssignmentContext.class,0);
		}
		public AssignmentStatementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterAssignmentStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitAssignmentStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitAssignmentStatement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class VarDeclarationStatementContext extends StatementContext {
		public VarDeclarationContext varDeclaration() {
			return getRuleContext(VarDeclarationContext.class,0);
		}
		public VarDeclarationStatementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterVarDeclarationStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitVarDeclarationStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitVarDeclarationStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_statement);
		try {
			setState(31);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VAR:
				_localctx = new VarDeclarationStatementContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(28);
				varDeclaration();
				}
				break;
			case ID:
				_localctx = new AssignmentStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(29);
				assignment();
				}
				break;
			case PRINT:
				_localctx = new PrintStatementContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(30);
				print();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentListContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(TnG2Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TnG2Parser.COMMA, i);
		}
		public ArgumentListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterArgumentList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitArgumentList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitArgumentList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentListContext argumentList() throws RecognitionException {
		ArgumentListContext _localctx = new ArgumentListContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_argumentList);
		int _la;
		try {
			setState(42);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RPAREN:
				enterOuterAlt(_localctx, 1);
				{
				}
				break;
			case SECOND:
			case MINUTE:
			case HOURLY:
			case DAILY:
			case WEEKLY:
			case MONTHLY:
			case YEARLY:
			case AVERAGE:
			case MEDIAN:
			case MIN:
			case MAX:
			case EARLIEST:
			case LATEST:
			case INTLIT:
			case DECLIT:
			case MINUS:
			case LPAREN:
			case FUNCTION_NAME:
			case ID:
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(34);
				expression(0);
				setState(39);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(35);
					match(COMMA);
					setState(36);
					expression(0);
					}
					}
					setState(41);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrintContext extends ParserRuleContext {
		public TerminalNode PRINT() { return getToken(TnG2Parser.PRINT, 0); }
		public TerminalNode LPAREN() { return getToken(TnG2Parser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(TnG2Parser.RPAREN, 0); }
		public PrintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_print; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterPrint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitPrint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitPrint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrintContext print() throws RecognitionException {
		PrintContext _localctx = new PrintContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_print);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(44);
			match(PRINT);
			setState(45);
			match(LPAREN);
			setState(46);
			expression(0);
			setState(47);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VarDeclarationContext extends ParserRuleContext {
		public TerminalNode VAR() { return getToken(TnG2Parser.VAR, 0); }
		public AssignmentContext assignment() {
			return getRuleContext(AssignmentContext.class,0);
		}
		public VarDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_varDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterVarDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitVarDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitVarDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarDeclarationContext varDeclaration() throws RecognitionException {
		VarDeclarationContext _localctx = new VarDeclarationContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_varDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			match(VAR);
			setState(50);
			assignment();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignmentContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(TnG2Parser.ID, 0); }
		public TerminalNode ASSIGN() { return getToken(TnG2Parser.ASSIGN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(52);
			match(ID);
			setState(53);
			match(ASSIGN);
			setState(54);
			expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	 
		public ExpressionContext() { }
		public void copyFrom(ExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DecimalLiteralContext extends ExpressionContext {
		public TerminalNode DECLIT() { return getToken(TnG2Parser.DECLIT, 0); }
		public DecimalLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterDecimalLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitDecimalLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitDecimalLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MinusExpressionContext extends ExpressionContext {
		public TerminalNode MINUS() { return getToken(TnG2Parser.MINUS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public MinusExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterMinusExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitMinusExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitMinusExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends ExpressionContext {
		public TerminalNode STRING() { return getToken(TnG2Parser.STRING, 0); }
		public StringLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FunctionCallContext extends ExpressionContext {
		public Token functionName;
		public ArgumentListContext args;
		public TerminalNode LPAREN() { return getToken(TnG2Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TnG2Parser.RPAREN, 0); }
		public TerminalNode FUNCTION_NAME() { return getToken(TnG2Parser.FUNCTION_NAME, 0); }
		public ArgumentListContext argumentList() {
			return getRuleContext(ArgumentListContext.class,0);
		}
		public FunctionCallContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntLiteralContext extends ExpressionContext {
		public TerminalNode INTLIT() { return getToken(TnG2Parser.INTLIT, 0); }
		public IntLiteralContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TimePeriodContext extends ExpressionContext {
		public Time_periodContext time_period() {
			return getRuleContext(Time_periodContext.class,0);
		}
		public TimePeriodContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterTimePeriod(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitTimePeriod(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitTimePeriod(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParenExpressionContext extends ExpressionContext {
		public TerminalNode LPAREN() { return getToken(TnG2Parser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(TnG2Parser.RPAREN, 0); }
		public ParenExpressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterParenExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitParenExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitParenExpression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BinaryOperationContext extends ExpressionContext {
		public ExpressionContext left;
		public Token operator;
		public ExpressionContext right;
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode DIVISION() { return getToken(TnG2Parser.DIVISION, 0); }
		public TerminalNode ASTERISK() { return getToken(TnG2Parser.ASTERISK, 0); }
		public TerminalNode PLUS() { return getToken(TnG2Parser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(TnG2Parser.MINUS, 0); }
		public BinaryOperationContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterBinaryOperation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitBinaryOperation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitBinaryOperation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AggregationFunctionContext extends ExpressionContext {
		public Aggregation_functionContext aggregation_function() {
			return getRuleContext(Aggregation_functionContext.class,0);
		}
		public AggregationFunctionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterAggregationFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitAggregationFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitAggregationFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class VarReferenceContext extends ExpressionContext {
		public TerminalNode ID() { return getToken(TnG2Parser.ID, 0); }
		public VarReferenceContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterVarReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitVarReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitVarReference(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 14;
		enterRecursionRule(_localctx, 14, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(74);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				{
				_localctx = new VarReferenceContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(57);
				match(ID);
				}
				break;
			case MINUS:
				{
				_localctx = new MinusExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(58);
				match(MINUS);
				setState(59);
				expression(8);
				}
				break;
			case INTLIT:
				{
				_localctx = new IntLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(60);
				match(INTLIT);
				}
				break;
			case DECLIT:
				{
				_localctx = new DecimalLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(61);
				match(DECLIT);
				}
				break;
			case SECOND:
			case MINUTE:
			case HOURLY:
			case DAILY:
			case WEEKLY:
			case MONTHLY:
			case YEARLY:
				{
				_localctx = new TimePeriodContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(62);
				time_period();
				}
				break;
			case AVERAGE:
			case MEDIAN:
			case MIN:
			case MAX:
			case EARLIEST:
			case LATEST:
				{
				_localctx = new AggregationFunctionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(63);
				aggregation_function();
				}
				break;
			case STRING:
				{
				_localctx = new StringLiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(64);
				match(STRING);
				}
				break;
			case FUNCTION_NAME:
				{
				_localctx = new FunctionCallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(65);
				((FunctionCallContext)_localctx).functionName = match(FUNCTION_NAME);
				setState(66);
				match(LPAREN);
				setState(67);
				((FunctionCallContext)_localctx).args = argumentList();
				setState(68);
				match(RPAREN);
				}
				break;
			case LPAREN:
				{
				_localctx = new ParenExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(70);
				match(LPAREN);
				setState(71);
				expression(0);
				setState(72);
				match(RPAREN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(84);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(82);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
					case 1:
						{
						_localctx = new BinaryOperationContext(new ExpressionContext(_parentctx, _parentState));
						((BinaryOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(76);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(77);
						((BinaryOperationContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==ASTERISK || _la==DIVISION) ) {
							((BinaryOperationContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(78);
						((BinaryOperationContext)_localctx).right = expression(12);
						}
						break;
					case 2:
						{
						_localctx = new BinaryOperationContext(new ExpressionContext(_parentctx, _parentState));
						((BinaryOperationContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(79);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(80);
						((BinaryOperationContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==PLUS || _la==MINUS) ) {
							((BinaryOperationContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(81);
						((BinaryOperationContext)_localctx).right = expression(11);
						}
						break;
					}
					} 
				}
				setState(86);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Time_periodContext extends ParserRuleContext {
		public Time_periodContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_time_period; }
	 
		public Time_periodContext() { }
		public void copyFrom(Time_periodContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class HourlyContext extends Time_periodContext {
		public TerminalNode HOURLY() { return getToken(TnG2Parser.HOURLY, 0); }
		public HourlyContext(Time_periodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterHourly(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitHourly(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitHourly(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DailyContext extends Time_periodContext {
		public TerminalNode DAILY() { return getToken(TnG2Parser.DAILY, 0); }
		public DailyContext(Time_periodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterDaily(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitDaily(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitDaily(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MonthlyContext extends Time_periodContext {
		public TerminalNode MONTHLY() { return getToken(TnG2Parser.MONTHLY, 0); }
		public MonthlyContext(Time_periodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterMonthly(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitMonthly(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitMonthly(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SecondContext extends Time_periodContext {
		public TerminalNode SECOND() { return getToken(TnG2Parser.SECOND, 0); }
		public SecondContext(Time_periodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterSecond(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitSecond(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitSecond(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MinuteContext extends Time_periodContext {
		public TerminalNode MINUTE() { return getToken(TnG2Parser.MINUTE, 0); }
		public MinuteContext(Time_periodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterMinute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitMinute(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitMinute(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WeeklyContext extends Time_periodContext {
		public TerminalNode WEEKLY() { return getToken(TnG2Parser.WEEKLY, 0); }
		public WeeklyContext(Time_periodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterWeekly(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitWeekly(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitWeekly(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class YearlyContext extends Time_periodContext {
		public TerminalNode YEARLY() { return getToken(TnG2Parser.YEARLY, 0); }
		public YearlyContext(Time_periodContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterYearly(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitYearly(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitYearly(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Time_periodContext time_period() throws RecognitionException {
		Time_periodContext _localctx = new Time_periodContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_time_period);
		try {
			setState(94);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SECOND:
				_localctx = new SecondContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(87);
				match(SECOND);
				}
				break;
			case MINUTE:
				_localctx = new MinuteContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(88);
				match(MINUTE);
				}
				break;
			case HOURLY:
				_localctx = new HourlyContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(89);
				match(HOURLY);
				}
				break;
			case DAILY:
				_localctx = new DailyContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(90);
				match(DAILY);
				}
				break;
			case WEEKLY:
				_localctx = new WeeklyContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(91);
				match(WEEKLY);
				}
				break;
			case MONTHLY:
				_localctx = new MonthlyContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(92);
				match(MONTHLY);
				}
				break;
			case YEARLY:
				_localctx = new YearlyContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(93);
				match(YEARLY);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Aggregation_functionContext extends ParserRuleContext {
		public Aggregation_functionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggregation_function; }
	 
		public Aggregation_functionContext() { }
		public void copyFrom(Aggregation_functionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MinContext extends Aggregation_functionContext {
		public TerminalNode MIN() { return getToken(TnG2Parser.MIN, 0); }
		public MinContext(Aggregation_functionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterMin(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitMin(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitMin(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MaxContext extends Aggregation_functionContext {
		public TerminalNode MAX() { return getToken(TnG2Parser.MAX, 0); }
		public MaxContext(Aggregation_functionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterMax(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitMax(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitMax(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AverageContext extends Aggregation_functionContext {
		public TerminalNode AVERAGE() { return getToken(TnG2Parser.AVERAGE, 0); }
		public AverageContext(Aggregation_functionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterAverage(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitAverage(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitAverage(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MedianContext extends Aggregation_functionContext {
		public TerminalNode MEDIAN() { return getToken(TnG2Parser.MEDIAN, 0); }
		public MedianContext(Aggregation_functionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterMedian(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitMedian(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitMedian(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EarliestContext extends Aggregation_functionContext {
		public TerminalNode EARLIEST() { return getToken(TnG2Parser.EARLIEST, 0); }
		public EarliestContext(Aggregation_functionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterEarliest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitEarliest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitEarliest(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LatestContext extends Aggregation_functionContext {
		public TerminalNode LATEST() { return getToken(TnG2Parser.LATEST, 0); }
		public LatestContext(Aggregation_functionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).enterLatest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TnG2Listener ) ((TnG2Listener)listener).exitLatest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TnG2Visitor ) return ((TnG2Visitor<? extends T>)visitor).visitLatest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Aggregation_functionContext aggregation_function() throws RecognitionException {
		Aggregation_functionContext _localctx = new Aggregation_functionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_aggregation_function);
		try {
			setState(102);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AVERAGE:
				_localctx = new AverageContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(96);
				match(AVERAGE);
				}
				break;
			case MEDIAN:
				_localctx = new MedianContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(97);
				match(MEDIAN);
				}
				break;
			case MIN:
				_localctx = new MinContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(98);
				match(MIN);
				}
				break;
			case MAX:
				_localctx = new MaxContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(99);
				match(MAX);
				}
				break;
			case EARLIEST:
				_localctx = new EarliestContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(100);
				match(EARLIEST);
				}
				break;
			case LATEST:
				_localctx = new LatestContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(101);
				match(LATEST);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 7:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 11);
		case 1:
			return precpred(_ctx, 10);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3#k\4\2\t\2\4\3\t\3"+
		"\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\3\2"+
		"\6\2\30\n\2\r\2\16\2\31\3\3\3\3\3\3\3\4\3\4\3\4\5\4\"\n\4\3\5\3\5\3\5"+
		"\3\5\7\5(\n\5\f\5\16\5+\13\5\5\5-\n\5\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7"+
		"\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\t\3\t\5\tM\n\t\3\t\3\t\3\t\3\t\3\t\3\t\7\tU\n\t\f\t\16\t"+
		"X\13\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\na\n\n\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\5\13i\n\13\3\13\2\3\20\f\2\4\6\b\n\f\16\20\22\24\2\5\3\3\3\3\3\2\33"+
		"\34\3\2\31\32\2z\2\27\3\2\2\2\4\33\3\2\2\2\6!\3\2\2\2\b,\3\2\2\2\n.\3"+
		"\2\2\2\f\63\3\2\2\2\16\66\3\2\2\2\20L\3\2\2\2\22`\3\2\2\2\24h\3\2\2\2"+
		"\26\30\5\4\3\2\27\26\3\2\2\2\30\31\3\2\2\2\31\27\3\2\2\2\31\32\3\2\2\2"+
		"\32\3\3\2\2\2\33\34\5\6\4\2\34\35\t\2\2\2\35\5\3\2\2\2\36\"\5\f\7\2\37"+
		"\"\5\16\b\2 \"\5\n\6\2!\36\3\2\2\2!\37\3\2\2\2! \3\2\2\2\"\7\3\2\2\2#"+
		"-\3\2\2\2$)\5\20\t\2%&\7#\2\2&(\5\20\t\2\'%\3\2\2\2(+\3\2\2\2)\'\3\2\2"+
		"\2)*\3\2\2\2*-\3\2\2\2+)\3\2\2\2,#\3\2\2\2,$\3\2\2\2-\t\3\2\2\2./\7\6"+
		"\2\2/\60\7\36\2\2\60\61\5\20\t\2\61\62\7\37\2\2\62\13\3\2\2\2\63\64\7"+
		"\5\2\2\64\65\5\16\b\2\65\r\3\2\2\2\66\67\7!\2\2\678\7\35\2\289\5\20\t"+
		"\29\17\3\2\2\2:;\b\t\1\2;M\7!\2\2<=\7\32\2\2=M\5\20\t\n>M\7\27\2\2?M\7"+
		"\30\2\2@M\5\22\n\2AM\5\24\13\2BM\7\"\2\2CD\7 \2\2DE\7\36\2\2EF\5\b\5\2"+
		"FG\7\37\2\2GM\3\2\2\2HI\7\36\2\2IJ\5\20\t\2JK\7\37\2\2KM\3\2\2\2L:\3\2"+
		"\2\2L<\3\2\2\2L>\3\2\2\2L?\3\2\2\2L@\3\2\2\2LA\3\2\2\2LB\3\2\2\2LC\3\2"+
		"\2\2LH\3\2\2\2MV\3\2\2\2NO\f\r\2\2OP\t\3\2\2PU\5\20\t\16QR\f\f\2\2RS\t"+
		"\4\2\2SU\5\20\t\rTN\3\2\2\2TQ\3\2\2\2UX\3\2\2\2VT\3\2\2\2VW\3\2\2\2W\21"+
		"\3\2\2\2XV\3\2\2\2Ya\7\n\2\2Za\7\13\2\2[a\7\f\2\2\\a\7\r\2\2]a\7\16\2"+
		"\2^a\7\17\2\2_a\7\20\2\2`Y\3\2\2\2`Z\3\2\2\2`[\3\2\2\2`\\\3\2\2\2`]\3"+
		"\2\2\2`^\3\2\2\2`_\3\2\2\2a\23\3\2\2\2bi\7\21\2\2ci\7\22\2\2di\7\23\2"+
		"\2ei\7\24\2\2fi\7\25\2\2gi\7\26\2\2hb\3\2\2\2hc\3\2\2\2hd\3\2\2\2he\3"+
		"\2\2\2hf\3\2\2\2hg\3\2\2\2i\25\3\2\2\2\13\31!),LTV`h";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}