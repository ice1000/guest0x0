# Guest0x0

![image](https://user-images.githubusercontent.com/16398479/161549473-ef24de7c-3033-4874-8354-54a960b3f873.png)

Experimenting with some basic programming in Java. "Basic programming" includes:

+ A "de Morgan flavored" cubical type theory with redtt flavored cubes (the so-called "extension types")
+ Inductive types with pattern matching (hopefully) and "simpler indices" (see my TyDe paper)
+ An equalizer of the first projection of evaluation and just the first projection :trollface:

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
