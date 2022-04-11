# Guest0x0

[![maven]](https://repo1.maven.org/maven2/org/aya-prover/guest0x0-base/)
[![test](https://github.com/ice1000/guest0x0/actions/workflows/gradle-check.yml/badge.svg)](https://github.com/ice1000/guest0x0/actions/workflows/gradle-check.yml)

![image](https://user-images.githubusercontent.com/16398479/162600129-d59a0c0c-de5a-49e4-b0dd-dd3c4fd51938.png)

[maven]: https://img.shields.io/maven-central/v/org.aya-prover/guest0x0-base

Experimenting with some basic programming in Java 17 and see the following:

+ If Java competes with existing established independently typed languages for writing compilers, like Haskell
+ If capture-avoiding substitution is not too bad (because with nestable pattern matching, de-bruijn indices are too bad)
  + Cody [told me](https://twitter.com/codydroux/status/1512204955641389056) that locally nameless is survivable, hmm
+ If my understanding of basic programming is appropriate

"Basic programming" includes:

+ A "de Morgan flavored" cubical type theory with redtt flavored cubes (the so-called "extension types")
+ A ✨new syntax✨ for cofibration theory that is more convenient for confluence checking and hopefully is equivalent to existing ones
+ Inductive types with pattern matching (hopefully) and "simpler indices" (see my TyDe paper)
+ An equalizer of the first projection of evaluation and just the first projection :trollface:

Make sure you listen to Suede or Deep Purple while looking at this project.

## Milestones

### v0.5

Interval connections and involution and de Morgan laws.

```
def sym (A : U) (a b : A) (p : Eq A a b) : Eq A b a => \i. p (~ i)
```

### v0.4

![image](https://user-images.githubusercontent.com/16398479/162601962-c77035cf-5858-45a5-a2c0-062830276210.png)

Implemented boundaries' confluence check and the new syntax for multidimensional cubes ("extension types"),
855 lines of Java now. The multi-case trees utilities from Aya tools are used, very neat.
Error message for holes is now more informative (displays shadowing information).
The following code has some endpoints do not agree:

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

Added some cosmetics features like pretty printing with precedence, one more conversion rule,
JPMS support, interval terms, a bunch of helper methods, and unicode keyword.
I wish to keep the project small, but with that we have 736 lines of Java now. That's a lot more.
Just one more theorem we can define:

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

Lambdas are overloaded as paths, and paths reduce according to the boundaries.
Total lines of Java code: 580, including blank/comments. The following are missing:

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

Minimal type checker with definitions, pi, sigma, and universe.
484 lines of Java code (including comments and blank lines and `import` statements in many files),
comparable to Mini-TT (Main.hs and Core/Abs.hs, 358 + 46 = 404 lines in Haskell).
But Mini-TT supports (a cursed version of) sum types.

```
def Eq (A : Type) (a : A) (b : A) : Type => Pi (P : A -> Type) -> P a -> P b
def refl (A : Type) (a : A) : Eq A a a => \P. \pa. pa
def sym (A : Type) (a : A) (b : A) (e : Eq A a b) : Eq A b a => e (\b. Eq A b a) (refl A a)
```
