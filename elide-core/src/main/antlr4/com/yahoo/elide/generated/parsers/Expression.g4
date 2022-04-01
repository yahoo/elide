/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
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
