package p;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

interface I0 <error descr="Invalid permits clause: 'I0' must be sealed">permits</error> I {}

sealed interface I extends I0 permits <error descr="Cannot resolve symbol 'Unresolved'">Unresolved</error>, <error descr="Duplicate class: 'p.I1'">I1</error>, <error descr="Duplicate class: 'p.I1'">I1</error>{}
non-sealed interface I1 extends I {}

sealed interface <error descr="Sealed class permits clause must contain all subclasses">A</error> {}
class Usage {
  {
    class Local implements <error descr="Local classes must not extend sealed classes">A</error> {}
    A a = new <error descr="Anonymous classes must not extend sealed classes">A</error>() {};
  }
}


sealed interface Indirect permits <error descr="Invalid permits clause: 'IndirectInheritor' must directly implement 'Indirect'">IndirectInheritor</error>, MiddleMan {}
non-sealed interface MiddleMan extends Indirect {}
final class IndirectInheritor implements MiddleMan {}

sealed class AnotherPackage permits <error descr="Class is not allowed to extend sealed class from another package">p1.P1</error> {}

enum ImlicitlySealedWithPermitsClause <error descr="'permits' not allowed on enum">permits</error> FOO {
  FOO {};
}

sealed class Parent permits TypedChild<error descr="Generics are not allowed in permits list"><Integer></error> {}

non-sealed class TypedChild<F> extends Parent {}

class TestWithAnnotations{
  @Target(ElementType.TYPE_USE)
  @interface Ann {
  }

  public static void main(String[] args) {

  }
  sealed class PermittedAnnotations permits <error descr="Annotation is not allowed in permit list">@Ann</error> SealedOne {
  }

  non-sealed class SealedOne extends PermittedAnnotations {
  }
}