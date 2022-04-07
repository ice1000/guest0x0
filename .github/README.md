# Guest0x0

![maven]

![image](https://user-images.githubusercontent.com/16398479/162101384-cebf6e0f-c0c4-4044-8dcc-291f86a0bc09.png)

[maven]: https://img.shields.io/maven-central/v/org.aya-prover/guest0x0-base

Experimenting with some basic programming in Java 17 and see the following:

+ If Java competes with existing established independently typed languages for writing compilers, like Haskell
+ If capture-avoiding substitution is not too bad (because with nestable pattern matching, de-bruijn indices are too bad)
+ If my understanding of basic programming is appropriate

"Basic programming" includes:

+ A "de Morgan flavored" cubical type theory with redtt flavored cubes (the so-called "extension types")
+ A ✨new syntax✨ for cofibration theory that is more convenient for confluence checking and hopefully is equivalent to existing ones
+ Inductive types with pattern matching (hopefully) and "simpler indices" (see my TyDe paper)
+ An equalizer of the first projection of evaluation and just the first projection :trollface:

Make sure you listen to Suede or Deep Purple while looking at this project.

## Milestones

### v0.1

Minimal type checker with definitions, pi, sigma, and universe.
484 lines of Java code (including comments and blank lines and `import` statements in many files),
comparable to Mini-TT (Main.hs and Core/Abs.hs, 358 + 46 = 404 lines in Haskell).
But Mini-TT supports (a cursed version of) sum types.

```
def Eq (A : Type) (a : A) (b : A) : Type => Pi (P : A -> Type) -> P a -> P b
def refl (A : Type) (a : A) : Eq A a a => \P. \pa. pa
def sym (A : Type) (a : A) (b : A) (e : Eq A a b) : Eq A b a => e (\b. Eq A b a) (refl A a)
```
