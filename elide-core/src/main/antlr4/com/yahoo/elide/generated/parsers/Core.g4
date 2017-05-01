/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

grammar Core;

start: rootCollection query;

rootCollection
    : term                      #rootCollectionLoadEntities
    | entity '/' relationship   #rootCollectionRelationship
    | entity '/' subCollection  #rootCollectionSubCollection
    | entity                    #rootCollectionLoadEntity
    ;

entity
    : term '/' id
    ;

subCollection
    : term                      #subCollectionReadCollection
    | entity '/' relationship   #subCollectionRelationship
    | entity '/' subCollection  #subCollectionSubCollection
    | entity                    #subCollectionReadEntity
    ;

relationship: RELATIONSHIPS '/' term;

query: ; // Visitor performs query and outputs result

term: PATHSTR;
id: PATHSTR;

RELATIONSHIPS: 'relationships';

PATHSTR: UNRESERVED+;

UNRESERVED
    : ALPHANUM
    | MARK
    ;

MARK
    : '-'
    | '_'
    | '.'
    | '!'
    | '~'
    | '*'
    | '\''
    | '('
    | ')'
    ;

ALPHANUM
      : ALPHA
      | DIGIT
      ;

ALPHA: [a-zA-Z];

DIGIT: [0-9];
