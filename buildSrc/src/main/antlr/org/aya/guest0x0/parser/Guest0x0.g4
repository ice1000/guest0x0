grammar Guest0x0;

program : decl+;
decl : fnDecl;
fnDecl : 'def' ID param* ':' expr '=>' expr;
param : '(' ID ':' expr ')';
expr
 // Type formers
 : 'Type' # trebor // McBride universe a la Trebor
 | <assoc=right> expr '->' expr # simpFun
 | 'Pi' param '->' expr # pi
 | 'Sig' param '**' expr # sig

 // Introduction lures
 | '\\' ID '.' expr # lam
 | '<<' expr ',' expr '>>' # pair

 // Elimination lures
 | expr expr # app
 | expr '.1' # fst
 | expr '.2' # snd

 // Others
 | ID # ref
 | '(' expr ')' # paren
 ;


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
