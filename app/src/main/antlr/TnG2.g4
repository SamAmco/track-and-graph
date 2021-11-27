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

dataTransformationFunction : ((line NEWLINE)|NEWLINE)* (line)? EOF ;

line                : ID ASSIGN expression                   #assignExp
                    | ID ASSIGN NAME '(' argList ')'         #assignFunc
                    ;

argList             : (arg ',')* (arg)? ;

arg                 : (ID|NUMBER) ;


expression          : '(' expression ')'                        #parenthesisExp
                    | expression (ASTERISK|SLASH) expression    #mulDivExp
                    | expression (PLUS|MINUS) expression        #addSubExp
                    | NUMBER                                    #numericAtomExp
                    | ID                                        #idAtomExp
                    ;
fragment LETTER     : [a-zA-Z] ;
fragment DIGIT      : [0-9] ;

ID                 : '.'[A-Za-z0-9]+ ;
NEWLINE            : '\r\n' | '\r' | '\n' ;
ASTERISK            : '*' ;
SLASH               : '/' ;
PLUS                : '+' ;
MINUS               : '-' ;
ASSIGN             : '=' ;
NAME                : LETTER+ ;
NUMBER              : DIGIT+ ('.' DIGIT+)? ;
WHITESPACE         : [\t ]+ -> skip ;