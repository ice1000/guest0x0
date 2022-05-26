# Guest0x0

[![maven]](https://repo1.maven.org/maven2/org/aya-prover/guest0x0-base/)
[![test](https://github.com/ice1000/guest0x0/actions/workflows/gradle-check.yml/badge.svg)](https://github.com/ice1000/guest0x0/actions/workflows/gradle-check.yml)

![image](https://user-images.githubusercontent.com/16398479/162600129-d59a0c0c-de5a-49e4-b0dd-dd3c4fd51938.png)

[maven]: https://img.shields.io/maven-central/v/org.aya-prover/guest0x0-base

Experimenting with some basic programming in Java 18 and see the following:

+ If Java competes with existing established independently typed languages for writing compilers, like Haskell
+ If capture-avoiding substitution is not too bad (because with nestable pattern matching, de-bruijn indices are too bad)
  + Cody [told me](https://twitter.com/codydroux/status/1512204955641389056) that locally nameless is survivable, hmm
+ If my understanding of basic programming is appropriate

"Basic programming" includes:

+ A "de Morgan flavored" cubical type theory with redtt flavored cubes (the so-called "extension types")
+ A ✨new syntax✨ for cofibration theory that is more convenient for confluence checking and hopefully is equivalent to existing ones
  + Update: it's not. The new syntax is implemented, tested, and removed for not working well with substitutions.
  + Unlike Cubical Agda, Guest0x0 sticks to the paper syntax, and is not constraint-based.
+ Inductive types with pattern matching (hopefully) and "simpler indices" (see my TyDe paper)
+ An equalizer of the first projection of evaluation and just the first projection :trollface:

Make sure you listen to Red Hot Chili Peppers while looking at this project.

## Milestones

### Progress

+ MLTT
  + [x] Capture-avoiding substitution
  + [x] Pi, Sigma, Universe
  + [x] Function definition and delta reduction
  + [ ] Typed conversion (bidirectional conversion checking)
  + [ ] Inductive type and pattern matching
+ CHM
  + [x] Extension type (generalized path type)
  + [x] Cofibration theory
  + [x] Partial elements (with a dedicated type like in Cubical Agda)
  + [ ] Generalized transport
    + [x] Pi, Sigma, Universe
    + [ ] Higher inductive type
    + [ ] Glue/V
  + [ ] Homogenous composition
    + [ ] Pi, Sigma, Universe
    + [ ] Higher inductive type
    + [ ] Glue/V

### Untagged

### v0.13

![image](https://user-images.githubusercontent.com/16398479/168640908-c1be4ea2-cc35-443d-ada9-84e754fef322.png)

Added a dedicated type for cofibrations (`F`), added partial element syntax `{| i = 0 |-> u | i = 1 /\ j = 0 |-> v |}` (this is a demonstration) and the dedicated type `Partial #{i = 0 \/ i = 1 /\ j = 0}`, like in Cubical Agda. Elimination of contradiction in cofibration is also implemented. Here are some examples of partial elements typing:

```
def par1 (A : Type) (u : A) (i : I) : Partial A #{i = 0} =>
  {| i = 0 |-> u |}
def par2 (A : Type) (u : A) (i : I) : Partial A #{i = 0} =>
  {| i = 0 |-> u | i = 1 |-> u |}
def par3 (A : Type) (u : A) (v : A) (i : I) : Partial A #{i = 0 \/ i = 1} =>
  {| i = 0 |-> u | i = 1 |-> v |}
def par4 (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 \/ i = 1 /\ j = 0} =>
  {| i = 0 |-> u | i = 1 /\ j = 0 |-> v |}
def par5 (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 \/ i = 1 /\ j = 0} =>
  {| i = 0 |-> u | i = 1 |-> v |}
```

Some counterexamples:

```
def par (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 /\ j = 0} =>
  {| i = 0 |-> u | i = 0 /\ j = 0 |-> v |}
```

Raises:

```
Boundaries disagree.
Umm, v != u on [| i = 0 |-> u | i = 0 /\ j = 0 |-> v |]
In particular, u != v
```

And this one:

```
def par (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 \/ i = 1} =>
  {| i = 0 |-> u | i = 1 /\ j = 0 |-> v |}
```

Raises:

```
The faces in the partial element i = 0 \/ (i = 1 /\ j = 0) must cover the face(s) specified
in type: i = 0 \/ i = 1
```

Total lines of Java code in base (including spaces and comments): 1322, and in cubical infra: 377.

### v0.12

![image](https://user-images.githubusercontent.com/16398479/166834889-07041e0e-db7b-41b4-9081-282ab40b8e70.png)

Changed the syntax of transport from `A #{cof}` to `tr A #{cof}` for future convenience, implemented many tools for systems. Added some tests for cofibration substitution, fixed some pretty-printing bugs. Below is a snippet that already works in old versions:

```
def transId (A : U) (a : A) : Eq A a (tr (\i. A) #{1=1} a) => refl A a
def forward (A : I -> Type) (r : I) : A r -> A 1 =>
  tr (\i. A (r \/ i)) #{r = 1}
```

### v0.11

![image](https://user-images.githubusercontent.com/16398479/166701476-d3cddf78-c621-4bbf-9b8a-9ef3f43d80b7.png)

The project is now split into three subprojects instead of two, and I believe that from now on counting the lines of Java code is pointless (as we are eventually going to grow). The new subproject `guest0x0-cubical` contains some generic utilities for face restrictions (cofibrations in cartesian cubical type theory) and boundaries. The constant check for generalized transport has finally been implemented :tada:. Consider the snippet below:

```
def trauma (A : I -> U) (a : A 0) (i : I) : A i => (\j. A (i /\ j)) #{i = 1} a
```

Under the cofibration `i = 1`, `A (i /\ j)` reduces to `A j` according to de Morgan laws, so Guest0x0 will reject the code with the following message:

```
The cover \j. A (i /\ j) has to be constant under the cofibration i = 1 but applying
a variable `?` to it results in A ? which contains a reference to `?`, oh no
```

Replacing the cofibration with `i = 0` makes `A (i /\ j)` reduce to `A 0`, which is a constant.

### v0.10

![image](https://user-images.githubusercontent.com/16398479/165893441-23c02472-d363-45fb-b460-9869552714a7.png)

Refactored the implementation of restrictions, finished splitting disjunctions. The number of lines of Java code is 1366 now, quite a lot. There aren't many new features, so no new code snippet this time.

### v0.9

![image](https://user-images.githubusercontent.com/16398479/163501950-c9820f2c-4b69-4133-ace8-2d561c298823.png)

Overhauled the cofibration syntax. It is now similar to a combination of CCHM and ABCFHL (I used `1=0`, `0=1` for empty face and `1=1`, `0=0` for truth face because I'm cool).

```
def trans (A : I -> U) (a : A 0) : A 1 => A #{0=1} a
def transPi (A : I -> U) (B : Pi (i : I) -> A i -> U)
    (f : Pi (x : A 0) -> B 0 x) : Pi (x : A 1) -> B 1 x =>
  \x. trans (\j. B j ((\i. A (j \/ ~ i)) #{j = 1} x))
    (f (trans (\i. A (~ i)) x))
```

Two things are yet to be done: split disjunctions in conjunctions and make `i = 0 \/ i = 1` false. Also, some small bugs are fixed (like dim vars are not removed from scope, causing bloated hole info) The codebase is growing larger and larger, to 1335 lines of code. Some small useful lemmata:

```
def subst (A : Type) (P : A -> Type) (p : I -> A)
          (lhs : P (p 0)) : P (p 1) =>
  trans (\i. P (p i)) lhs

def =-trans (A : Type) (p : I -> A) (q : [| i |] A { | 0 => p 1 })
    : [| i |] A { | 0 => p 0 | 1 => q 1 } =>
  subst A (Eq A (p 0)) (\i. q i) (\i. p i)
```

### v0.8

![image](https://user-images.githubusercontent.com/16398479/163415006-4c7ecf02-2ed1-4c8a-b3f6-779538401973.png)

Updated the CLI frontend to hide stack traces by default. Fixed some core theory bugs (many thanks to Amélia, MBones, and Daniel for their helps), implemented structural lures for universe, Pi, and Sigma types. 1179 lines of Java. I'm sort of giving up on lines of code thingies -- I don't want to sacrifice readability.

```
def transPiEq (A : I -> U) (B : Pi (i : I) -> A i -> U)
    : Eq ((Pi (x : A 0) -> B 0 x) -> (Pi (x : A 1) -> B 1 x))
         (transPi A B)
         (trans (\i. Pi (x : A i) -> B i x))
    => \i. transPi A B
def transSigma (A : I -> U) (B : Pi (i : I) -> A i -> U)
    (t : Sig (x : A 0) ** B 0 x) : Sig (x : A 1) ** B 1 x =>
  << trans A (t.1),
     trans (\j. B j ((\i. A (j /\ i)) ~@ j { | 0 } (t.1))) (t.2) >>
def transSigmaEq (A : I -> U) (B : Pi (i : I) -> A i -> U)
    : Eq ((Sig (x : A 0) ** B 0 x) -> (Sig (x : A 1) ** B 1 x))
         (transSigma A B)
         (trans (\i. Sig (x : A i) ** B i x))
    => \i. transSigma A B
```

### v0.7

![image](https://user-images.githubusercontent.com/16398479/163258018-ee80a9f9-2fa1-45cb-b336-bc493d97a6ae.png)

A work-in-progress `transp` implementation, denoted `~@` (very beautiful with JetBrains Mono or Fira Code). The syntax is adapted from the new cofibration theory syntax. 1027 lines of Java, good.

The `IsOne` constraint used in Cubical Agda is expressed as "the instantiations of a set of dimension variables match a pattern", and the type checking criteria becomes "the type line instantiated to every set of patterns gives the constant function". When matched, `transp` computes as identity. Other lures are yet unimplemented.

We now have the ability to test some de Morgan laws:

```
def trans (A : I -> U) (a : A 0) : A 1 => A ~@ {} a
def trans^-1 (A : I -> U) (a : A 1) : A 0 => (\i. A (~ i)) ~@ {} a
def transFn (A B : I -> U) (f : A 0 -> B 0) : A 1 -> B 1 =>
  \a. trans B (f (trans (\i. A (~ i)) a))
def transPi (A : I -> U) (B : Pi (i : I) -> A i -> U)
  (f : Pi (x : A 0) -> B 0 x) : Pi (x : A 1) -> B 1 x =>
    \x. trans (\j. B j ((\i. A (j \/ ~ i)) ~@ j { | 1 } x))
         (f (trans (\i. A (~ i)) x))
```

### v0.6

![image](https://user-images.githubusercontent.com/16398479/162851016-86f3f199-7ec8-42b7-adac-c43bbcc5ec3e.png)

Fixed many bugs in the previous version, including proper conversion lures for path application and full de morgan laws. 924 lines of Java. These functions are now accepted:

```
def rotate (A : U) (a b : A) (p q : Eq A a b)
           (s : Eq (Eq A a b) p q)
  : Eq (Eq A b a) (sym A a b q) (sym A a b p)
  => \i j. s (~ i) (~ j)
def minSq (A : U) (a b : A) (p : Eq A a b)
  : [| i j |] A { | 0 _ => a | 1 0 => a | 1 1 => b }
  => \i j. p (i /\ j)
def maxSq (A : U) (a b : A) (p : Eq A a b)
  : [| i j |] A { | 0 0 => a | 1 0 => b | _ 1 => b }
  => \i j. p (i \/ j)
```

Also, a simple CLI interface is implemented.

### v0.5

![image](https://user-images.githubusercontent.com/16398479/162815822-e22a0538-7185-4585-b53d-b7feaedda47d.png)

Interval connections and involution and de Morgan laws. 915 lines of Java code. Time to implement `transp`. I decide to design a new syntax for it, because I think of it as the elimination principle of `I`, and it has a special typing rule (i.e. you can't alias it as a function).

```
def sym (A : U) (a b : A) (p : Eq A a b) : Eq A b a => \i. p (~ i)
```

### v0.4

![image](https://user-images.githubusercontent.com/16398479/162601962-c77035cf-5858-45a5-a2c0-062830276210.png)

Implemented boundaries' confluence check and the new syntax for multidimensional cubes ("extension types"), 855 lines of Java now. The multi-case trees utilities from Aya tools are used, very neat. Error message for holes is now more informative (displays shadowing information). The following code has some endpoints do not agree:

```
def Guest0x0 (A : U) (a b c d : A)
             (ab : Eq A a b) (cd : Eq A c d) : U =>
   [| i j |] A { | 0 _ => ab j -- 0 0 => a, 0 1 => b
                 | 1 _ => cd j -- 1 0 => c, 1 1 => d
                 | 1 1 => a } -- violation
```

Guest0x0 will reject the above with `The 3rd and 2nd boundaries do not agree!!`.

### v0.3

![image](https://user-images.githubusercontent.com/16398479/162591730-6f218433-a26c-467f-b6f8-3dfc6ef7c0fe.png)

Added some cosmetics features like pretty printing with precedence, one more conversion rule, JPMS support, interval terms, a bunch of helper methods, and unicode keyword. I wish to keep the project small, but with that we have 736 lines of Java now. That's a lot more. Just one more theorem we can define:

```
def pmap (A B : U) (f : A -> B) (a b : A) (p : Eq A a b)
    : Eq B (f a) (f b) => \i. f (p i)
```

This is what Guest0x0 says about the `funExt` function body:

```
f : Pi (_ : A) → B
i : I
p : Pi (a : A) → [| j |] B {
  | 0 ⇒ f a
  | 1 ⇒ g a
}
B : U
j : I
g : Pi (_ : A) → B
A : U
----------------------------------
(\A. \a. \b. [| j |] A {
  | 0 ⇒ a
  | 1 ⇒ b
}) (Pi (_ : A) → B) f g
|→
[| j |] Pi (_ : A) → B {
  | 0 ⇒ f
  | 1 ⇒ g
}
```

### v0.2

![image](https://user-images.githubusercontent.com/16398479/162101384-cebf6e0f-c0c4-4044-8dcc-291f86a0bc09.png)

Lambdas are overloaded as paths, and paths reduce according to the boundaries. Total lines of Java code: 580, including blank/comments. The following are missing:

+ Confluence check (boundaries need to agree to be a well-formed cube)
+ Interval connections and endpoints (no parsing yet)
+ Higher-dimensional extension types (unhandled case in elaborator)
+ Type-directed eta conversion/expansion (currently term-directed in the unifier)

```
def Eq (A : U) (a b : A) : U => [| j |] A { | 0 => a | 1 => b }
def refl (A : U) (a : A) : Eq A a a => \i. a
def funExt (A B : U) (f g : A -> B)
           (p : Pi (a : A) -> Eq B (f a) (g a))
    : Eq (A -> B) f g => \i a. p a i
```

### v0.1

![image](https://user-images.githubusercontent.com/16398479/161549473-ef24de7c-3033-4874-8354-54a960b3f873.png)

Minimal type checker with definitions, pi, sigma, and universe. 484 lines of Java code (including comments and blank lines and `import` statements in many files), comparable to Mini-TT (Main.hs and Core/Abs.hs, 358 + 46 = 404 lines in Haskell). But Mini-TT supports (a cursed version of) sum types.

```
def Eq (A : Type) (a : A) (b : A) : Type => Pi (P : A -> Type) -> P a -> P b
def refl (A : Type) (a : A) : Eq A a a => \P. \pa. pa
def sym (A : Type) (a : A) (b : A) (e : Eq A a b) : Eq A b a => e (\b. Eq A b a) (refl A a)
```
