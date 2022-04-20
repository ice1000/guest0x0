grammar Guest0x0;

program : decl+;
decl
 : 'def' ID param* ':' expr ARROW2 expr # fnDecl
 ;
param : '(' ID+ ':' expr ')';
expr
 // Elimination lures
 : expr expr # two
 | expr '.1' # fst
 | expr '.2' # snd

 // Type formers
 | (UNIV | INTERVAL | FACE_TY) # keyword
 | <assoc=right> expr ARROW expr # simpFun
 | <assoc=right> expr TIMES expr # simpTup
 | PI param ARROW expr # pi
 | SIG param TIMES expr # sig

 // Introduction lures
 | LAM ID+ '.' expr # lam
 | LPAIR expr ',' expr RPAIR # pair

 // Others
 | ID # ref
 | '(' expr ')' # paren

 // Cubical features
 | '[|' ID+ '|]' expr '{' boundary* '}' # cube
 | expr '#{' psi '}' # trans
 | iPat # iLit
 | '~' expr # inv
 | expr (AND | OR) expr # iConn
 ;

cond : ID '=' (LEFT | RIGHT);
cof : cond (AND cond)*;
psi : cof (OR cof)* | TRUTH | ABSURD;
iPat : LEFT | RIGHT | '_';
AND : '/\\' | '\u2227';
OR : '\\/' | '\u2228';
TRUTH : '0=0' | '1=1';
ABSURD : '1=0' | '0=1';
boundary : '|' iPat* ARROW2 expr;

LPAIR : '<<';
RPAIR : '>>';
ARROW : '->' | '\u2192';
ARROW2 : '=>' | '\u21D2';
TIMES : '**' | '\u00D7';
SIG : 'Sig' | '\u03A3';
LAM : '\\' | '\u03BB';
PI : 'Pi' | '\u03A0';
RIGHT : '1';
LEFT : '0';
UNIV : 'U' | 'Type';
INTERVAL : 'I';
FACE_TY : 'F';

// Below are copy-and-paste from Aya. Plagiarism!! LOL

// identifier
fragment AYA_SIMPLE_LETTER : [~!@#$%^&*+=<>?/|[\u005Da-zA-Z_\u2200-\u22FF];
fragment AYA_UNICODE : [\u0080-\uFEFE] | [\uFF00-\u{10FFFF}]; // exclude U+FEFF which is a truly invisible char
fragment AYA_LETTER : AYA_SIMPLE_LETTER | AYA_UNICODE;
fragment AYA_LETTER_FOLLOW : AYA_LETTER | [0-9'-];
REPL_COMMAND : ':' AYA_LETTER_FOLLOW+;
ID : AYA_LETTER AYA_LETTER_FOLLOW* | '-' AYA_LETTER AYA_LETTER_FOLLOW*;

// whitespaces
WS : [ \t\r\n]+ -> channel(HIDDEN);
fragment COMMENT_CONTENT : ~[\r\n]*;
DOC_COMMENT : '--|' COMMENT_CONTENT;
LINE_COMMENT : '--' COMMENT_CONTENT -> channel(HIDDEN);
COMMENT : '{-' (COMMENT|.)*? '-}' -> channel(HIDDEN);

// avoid token recognition error in REPL
ERROR_CHAR : .;
