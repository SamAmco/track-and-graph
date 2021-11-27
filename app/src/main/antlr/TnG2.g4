/*
* This file is part of Track & Graph
*
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/

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