//   Copyright 2016 Federico Tomassetti
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

//  Modified

grammar TnG2;
@header {
package com.samco.trackandgraph.antlr.generated;
}


//options { tokenVocab=TnG2; }

datatransformationFunction : lines=line+ ;

line      : statement (NEWLINE | EOF) ;

statement : varDeclaration # varDeclarationStatement
          | assignment     # assignmentStatement
          | print          # printStatement ;


//functionCall : FUNCTION_NAME LPAREN argumentList RPAREN;

argumentList :

        | expression ( COMMA expression )* ;

print : PRINT LPAREN expression RPAREN ;

varDeclaration : VAR assignment ;

assignment : ID ASSIGN expression ;

expression : left=expression operator=(DIVISION|ASTERISK) right=expression # binaryOperation
           | left=expression operator=(PLUS|MINUS) right=expression        # binaryOperation
//           | value=expression AS targetType=type                           # typeConversion

           | ID                                                            # varReference
           | MINUS expression                                              # minusExpression
           | INTLIT                                                        # intLiteral
           | DECLIT                                                        # decimalLiteral
           | time_period                                                   # timePeriod
           | aggregation_function                                          # aggregationFunction
           | STRING                                                        # stringLiteral
           | functionName=FUNCTION_NAME LPAREN args=argumentList RPAREN    # functionCall
           | LPAREN expression RPAREN                                      # parenExpression ;


//type : INT       # integer
//     | DECIMAL   # decimal
//     | time_unit # timeUnit;

time_period:
      SECOND        # Second
    | MINUTE        # Minute
    | HOURLY        # Hourly
    | DAILY         # Daily
    | WEEKLY        # Weekly
    | MONTHLY       # Monthly
    | YEARLY        # Yearly ;

aggregation_function:
      AVERAGE       # Average
    | MEDIAN        # Median
    | MIN           # Min
    | MAX           # Max
    | EARLIEST      # Earliest
    | LATEST        # Latest ;



// Whitespace
NEWLINE            : '\r\n' | 'r' | '\n' ;
WS                 : [\t ]+ -> skip ;

// Keywords
VAR                : 'var' ;
PRINT              : 'print';
AS                 : 'as';
INT                : 'Int';
DECIMAL            : 'Decimal';

// Keywords, time units
SECOND             : 'SECOND' ;
MINUTE             : 'MINUTE' ;
HOURLY             : 'HOURLY'   | 'HOUR' ;
DAILY              : 'DAILY'    | 'DAY' ;
WEEKLY             : 'WEEKLY'   | 'WEEK' ;
MONTHLY            : 'MONTHLY'  | 'MONTH' ;
YEARLY             : 'YEARLY'   | 'YEAR' ;

// Keywords, aggregate functions
AVERAGE             : 'AVERAGE' ;
MEDIAN              : 'MEDIAN' ;
MIN                 : 'MIN' ;
MAX                 : 'MAX' ;
EARLIEST            : 'EARLIEST' ;
LATEST              : 'LATEST' ;

// Literals
INTLIT             : '0'|[1-9][0-9]* ;
DECLIT             : '0'|[1-9][0-9]* '.' [0-9]+ ;

// Operators
PLUS               : '+' ;
MINUS              : '-' ;
ASTERISK           : '*' ;
DIVISION           : '/' ;
ASSIGN             : '=' ;
LPAREN             : '(' ;
RPAREN             : ')' ;

// Identifiers
FUNCTION_NAME      : [A-Z][A-Za-z0-9_]* ;

ID                 : [_]*[a-z][A-Za-z0-9_]* ;

STRING             : '"' .*? '"' ;

COMMA              : ',' ;
