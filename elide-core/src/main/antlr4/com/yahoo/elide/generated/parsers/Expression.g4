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
    : NOT WS+ expression                              #NOT
    | LPAREN WS* expression WS* RPAREN                #PAREN
    | left=expression WS+ AND WS+ right=expression    #AND
    | left=expression WS+  OR WS+ right=expression    #OR
    | permissionClass                                 #PERMISSION
    ;

/*
This should be a (potentially fully qualified) class name.
*/
permissionClass: LEGAL_NAME (WS+ LEGAL_NAME)*;

NOT         : [Nn][Oo][Tt];
AND         : [Aa][Nn][Dd];
OR          :     [Oo][Rr];

LPAREN      : '(';
RPAREN      : ')';
WS          : ' ' | '\t';
LEGAL_NAME  : ALPHA LEGAL_CHAR*;
ALPHA       : [a-zA-Z];
LEGAL_CHAR  : ALPHA | '.' | [0-9] | '$';
