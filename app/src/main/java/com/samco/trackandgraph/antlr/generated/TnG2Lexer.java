package com.samco.trackandgraph.antlr.generated;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TnG2Lexer extends Lexer {
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
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"NEWLINE", "WS", "VAR", "PRINT", "AS", "INT", "DECIMAL", "SECOND", "MINUTE", 
			"HOURLY", "DAILY", "WEEKLY", "MONTHLY", "YEARLY", "AVERAGE", "MEDIAN", 
			"MIN", "MAX", "EARLIEST", "LATEST", "INTLIT", "DECLIT", "PLUS", "MINUS", 
			"ASTERISK", "DIVISION", "ASSIGN", "LPAREN", "RPAREN", "FUNCTION_NAME", 
			"ID", "STRING", "COMMA"
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


	public TnG2Lexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TnG2.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2#\u0122\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\3\2\3\2\3\2\5\2I\n\2\3\3\6\3L\n\3\r\3\16\3M\3\3\3\3\3\4\3"+
		"\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3"+
		"\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u0083"+
		"\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\5\f\u008d\n\f\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\5\r\u0099\n\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\5\16\u00a7\n\16\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\5\17\u00b3\n\17\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23"+
		"\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25"+
		"\3\25\3\25\3\25\3\25\3\26\3\26\3\26\7\26\u00df\n\26\f\26\16\26\u00e2\13"+
		"\26\5\26\u00e4\n\26\3\27\3\27\3\27\7\27\u00e9\n\27\f\27\16\27\u00ec\13"+
		"\27\3\27\3\27\6\27\u00f0\n\27\r\27\16\27\u00f1\5\27\u00f4\n\27\3\30\3"+
		"\30\3\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3"+
		"\37\7\37\u0106\n\37\f\37\16\37\u0109\13\37\3 \7 \u010c\n \f \16 \u010f"+
		"\13 \3 \3 \7 \u0113\n \f \16 \u0116\13 \3!\3!\7!\u011a\n!\f!\16!\u011d"+
		"\13!\3!\3!\3\"\3\"\3\u011b\2#\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13"+
		"\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61"+
		"\32\63\33\65\34\67\359\36;\37= ?!A\"C#\3\2\n\4\2\f\ftt\4\2\13\13\"\"\3"+
		"\2\63;\3\2\62;\3\2C\\\6\2\62;C\\aac|\3\2aa\3\2c|\2\u0131\2\3\3\2\2\2\2"+
		"\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2"+
		"\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2"+
		"\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2"+
		"\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2"+
		"\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2"+
		"\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\3H\3\2\2\2\5K\3\2\2\2\7Q\3\2\2\2\t"+
		"U\3\2\2\2\13[\3\2\2\2\r^\3\2\2\2\17b\3\2\2\2\21j\3\2\2\2\23q\3\2\2\2\25"+
		"\u0082\3\2\2\2\27\u008c\3\2\2\2\31\u0098\3\2\2\2\33\u00a6\3\2\2\2\35\u00b2"+
		"\3\2\2\2\37\u00b4\3\2\2\2!\u00bc\3\2\2\2#\u00c3\3\2\2\2%\u00c7\3\2\2\2"+
		"\'\u00cb\3\2\2\2)\u00d4\3\2\2\2+\u00e3\3\2\2\2-\u00f3\3\2\2\2/\u00f5\3"+
		"\2\2\2\61\u00f7\3\2\2\2\63\u00f9\3\2\2\2\65\u00fb\3\2\2\2\67\u00fd\3\2"+
		"\2\29\u00ff\3\2\2\2;\u0101\3\2\2\2=\u0103\3\2\2\2?\u010d\3\2\2\2A\u0117"+
		"\3\2\2\2C\u0120\3\2\2\2EF\7\17\2\2FI\7\f\2\2GI\t\2\2\2HE\3\2\2\2HG\3\2"+
		"\2\2I\4\3\2\2\2JL\t\3\2\2KJ\3\2\2\2LM\3\2\2\2MK\3\2\2\2MN\3\2\2\2NO\3"+
		"\2\2\2OP\b\3\2\2P\6\3\2\2\2QR\7x\2\2RS\7c\2\2ST\7t\2\2T\b\3\2\2\2UV\7"+
		"r\2\2VW\7t\2\2WX\7k\2\2XY\7p\2\2YZ\7v\2\2Z\n\3\2\2\2[\\\7c\2\2\\]\7u\2"+
		"\2]\f\3\2\2\2^_\7K\2\2_`\7p\2\2`a\7v\2\2a\16\3\2\2\2bc\7F\2\2cd\7g\2\2"+
		"de\7e\2\2ef\7k\2\2fg\7o\2\2gh\7c\2\2hi\7n\2\2i\20\3\2\2\2jk\7U\2\2kl\7"+
		"G\2\2lm\7E\2\2mn\7Q\2\2no\7P\2\2op\7F\2\2p\22\3\2\2\2qr\7O\2\2rs\7K\2"+
		"\2st\7P\2\2tu\7W\2\2uv\7V\2\2vw\7G\2\2w\24\3\2\2\2xy\7J\2\2yz\7Q\2\2z"+
		"{\7W\2\2{|\7T\2\2|}\7N\2\2}\u0083\7[\2\2~\177\7J\2\2\177\u0080\7Q\2\2"+
		"\u0080\u0081\7W\2\2\u0081\u0083\7T\2\2\u0082x\3\2\2\2\u0082~\3\2\2\2\u0083"+
		"\26\3\2\2\2\u0084\u0085\7F\2\2\u0085\u0086\7C\2\2\u0086\u0087\7K\2\2\u0087"+
		"\u0088\7N\2\2\u0088\u008d\7[\2\2\u0089\u008a\7F\2\2\u008a\u008b\7C\2\2"+
		"\u008b\u008d\7[\2\2\u008c\u0084\3\2\2\2\u008c\u0089\3\2\2\2\u008d\30\3"+
		"\2\2\2\u008e\u008f\7Y\2\2\u008f\u0090\7G\2\2\u0090\u0091\7G\2\2\u0091"+
		"\u0092\7M\2\2\u0092\u0093\7N\2\2\u0093\u0099\7[\2\2\u0094\u0095\7Y\2\2"+
		"\u0095\u0096\7G\2\2\u0096\u0097\7G\2\2\u0097\u0099\7M\2\2\u0098\u008e"+
		"\3\2\2\2\u0098\u0094\3\2\2\2\u0099\32\3\2\2\2\u009a\u009b\7O\2\2\u009b"+
		"\u009c\7Q\2\2\u009c\u009d\7P\2\2\u009d\u009e\7V\2\2\u009e\u009f\7J\2\2"+
		"\u009f\u00a0\7N\2\2\u00a0\u00a7\7[\2\2\u00a1\u00a2\7O\2\2\u00a2\u00a3"+
		"\7Q\2\2\u00a3\u00a4\7P\2\2\u00a4\u00a5\7V\2\2\u00a5\u00a7\7J\2\2\u00a6"+
		"\u009a\3\2\2\2\u00a6\u00a1\3\2\2\2\u00a7\34\3\2\2\2\u00a8\u00a9\7[\2\2"+
		"\u00a9\u00aa\7G\2\2\u00aa\u00ab\7C\2\2\u00ab\u00ac\7T\2\2\u00ac\u00ad"+
		"\7N\2\2\u00ad\u00b3\7[\2\2\u00ae\u00af\7[\2\2\u00af\u00b0\7G\2\2\u00b0"+
		"\u00b1\7C\2\2\u00b1\u00b3\7T\2\2\u00b2\u00a8\3\2\2\2\u00b2\u00ae\3\2\2"+
		"\2\u00b3\36\3\2\2\2\u00b4\u00b5\7C\2\2\u00b5\u00b6\7X\2\2\u00b6\u00b7"+
		"\7G\2\2\u00b7\u00b8\7T\2\2\u00b8\u00b9\7C\2\2\u00b9\u00ba\7I\2\2\u00ba"+
		"\u00bb\7G\2\2\u00bb \3\2\2\2\u00bc\u00bd\7O\2\2\u00bd\u00be\7G\2\2\u00be"+
		"\u00bf\7F\2\2\u00bf\u00c0\7K\2\2\u00c0\u00c1\7C\2\2\u00c1\u00c2\7P\2\2"+
		"\u00c2\"\3\2\2\2\u00c3\u00c4\7O\2\2\u00c4\u00c5\7K\2\2\u00c5\u00c6\7P"+
		"\2\2\u00c6$\3\2\2\2\u00c7\u00c8\7O\2\2\u00c8\u00c9\7C\2\2\u00c9\u00ca"+
		"\7Z\2\2\u00ca&\3\2\2\2\u00cb\u00cc\7G\2\2\u00cc\u00cd\7C\2\2\u00cd\u00ce"+
		"\7T\2\2\u00ce\u00cf\7N\2\2\u00cf\u00d0\7K\2\2\u00d0\u00d1\7G\2\2\u00d1"+
		"\u00d2\7U\2\2\u00d2\u00d3\7V\2\2\u00d3(\3\2\2\2\u00d4\u00d5\7N\2\2\u00d5"+
		"\u00d6\7C\2\2\u00d6\u00d7\7V\2\2\u00d7\u00d8\7G\2\2\u00d8\u00d9\7U\2\2"+
		"\u00d9\u00da\7V\2\2\u00da*\3\2\2\2\u00db\u00e4\7\62\2\2\u00dc\u00e0\t"+
		"\4\2\2\u00dd\u00df\t\5\2\2\u00de\u00dd\3\2\2\2\u00df\u00e2\3\2\2\2\u00e0"+
		"\u00de\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00e4\3\2\2\2\u00e2\u00e0\3\2"+
		"\2\2\u00e3\u00db\3\2\2\2\u00e3\u00dc\3\2\2\2\u00e4,\3\2\2\2\u00e5\u00f4"+
		"\7\62\2\2\u00e6\u00ea\t\4\2\2\u00e7\u00e9\t\5\2\2\u00e8\u00e7\3\2\2\2"+
		"\u00e9\u00ec\3\2\2\2\u00ea\u00e8\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00ed"+
		"\3\2\2\2\u00ec\u00ea\3\2\2\2\u00ed\u00ef\7\60\2\2\u00ee\u00f0\t\5\2\2"+
		"\u00ef\u00ee\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\u00ef\3\2\2\2\u00f1\u00f2"+
		"\3\2\2\2\u00f2\u00f4\3\2\2\2\u00f3\u00e5\3\2\2\2\u00f3\u00e6\3\2\2\2\u00f4"+
		".\3\2\2\2\u00f5\u00f6\7-\2\2\u00f6\60\3\2\2\2\u00f7\u00f8\7/\2\2\u00f8"+
		"\62\3\2\2\2\u00f9\u00fa\7,\2\2\u00fa\64\3\2\2\2\u00fb\u00fc\7\61\2\2\u00fc"+
		"\66\3\2\2\2\u00fd\u00fe\7?\2\2\u00fe8\3\2\2\2\u00ff\u0100\7*\2\2\u0100"+
		":\3\2\2\2\u0101\u0102\7+\2\2\u0102<\3\2\2\2\u0103\u0107\t\6\2\2\u0104"+
		"\u0106\t\7\2\2\u0105\u0104\3\2\2\2\u0106\u0109\3\2\2\2\u0107\u0105\3\2"+
		"\2\2\u0107\u0108\3\2\2\2\u0108>\3\2\2\2\u0109\u0107\3\2\2\2\u010a\u010c"+
		"\t\b\2\2\u010b\u010a\3\2\2\2\u010c\u010f\3\2\2\2\u010d\u010b\3\2\2\2\u010d"+
		"\u010e\3\2\2\2\u010e\u0110\3\2\2\2\u010f\u010d\3\2\2\2\u0110\u0114\t\t"+
		"\2\2\u0111\u0113\t\7\2\2\u0112\u0111\3\2\2\2\u0113\u0116\3\2\2\2\u0114"+
		"\u0112\3\2\2\2\u0114\u0115\3\2\2\2\u0115@\3\2\2\2\u0116\u0114\3\2\2\2"+
		"\u0117\u011b\7$\2\2\u0118\u011a\13\2\2\2\u0119\u0118\3\2\2\2\u011a\u011d"+
		"\3\2\2\2\u011b\u011c\3\2\2\2\u011b\u0119\3\2\2\2\u011c\u011e\3\2\2\2\u011d"+
		"\u011b\3\2\2\2\u011e\u011f\7$\2\2\u011fB\3\2\2\2\u0120\u0121\7.\2\2\u0121"+
		"D\3\2\2\2\23\2HM\u0082\u008c\u0098\u00a6\u00b2\u00e0\u00e3\u00ea\u00f1"+
		"\u00f3\u0107\u010d\u0114\u011b\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}