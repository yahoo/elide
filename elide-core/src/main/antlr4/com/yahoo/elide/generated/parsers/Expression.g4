/*
 * Copyright (c) 2016 Yahoo! Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */

grammar Expression;

start: expression;

/*
Doesn't work with single character names.
*/

expression
    :
    NOT expression                      #NOT
    | LPAREN WS? expression WS? RPAREN  #PAREN
    | expression WS? AND WS? expression #AND
    | expression WS? OR WS? expression  #OR
    | expressionClass                   #EXPRESSION
    ;

/*
No only numeric because class names can't be only numeric.

Do not name this 'class' as that is a keyword.
*/
expressionClass: ALPHANUMERIC*;

NOT: [Nn][Oo][Tt];
AND: [Aa][Nn][Dd];
OR: [Oo][Rr];

LPAREN : '(' -> channel(HIDDEN);
RPAREN : ')' -> channel(HIDDEN);
ALPHA: [a-zA-Z];
DIGIT: [0-9];
ALPHANUMERIC: [a-zA-Z0-9]+;
WS: (' ' | '\t') -> channel(HIDDEN);