/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary check for duplicate annotations
 * @author Mahmood Ali
 * @compile/fail/ref=DuplicateTypeAnnotation.out -XDrawDiagnostics DuplicateTypeAnnotation.java
 */
import java.lang.annotation.*;
class DuplicateTypeAnno<K> {
  DuplicateTypeAnno<@A @A String> l;
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
