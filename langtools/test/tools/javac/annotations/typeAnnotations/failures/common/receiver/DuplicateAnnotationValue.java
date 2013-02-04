/*
 * @test /nodynamiccopyright/
 * @bug 6843077 6919944 8006775
 * @summary check for duplicate annotation values in receiver
 * @author Mahmood Ali
 * @compile/fail/ref=DuplicateAnnotationValue.out -XDrawDiagnostics DuplicateAnnotationValue.java
 */
import java.lang.annotation.*;
class DuplicateAnnotationValue {
  void test(@A(value = 2, value = 1) DuplicateAnnotationValue this) { }
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { int value(); }
