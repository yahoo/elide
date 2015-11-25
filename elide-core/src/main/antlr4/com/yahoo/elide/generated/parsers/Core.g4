/*
 * Copyright (c) 2015 Yahoo! Inc. All Rights Reserved.
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
