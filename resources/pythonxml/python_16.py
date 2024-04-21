class A:
    def a():
        return 1
    
class B:
    def b():
        return 1

class C:
    def c():
        return 1

class D:
    def d():
        return 1
    
class E:
    def e():
        return 1
    
class F(A, B, C):
    def f():
        return 1
    
class G(D, E):
    def g():
        return 1
    
class H(F, G):
    def a():
        return 1
    def b():
        return 1
    def c():
        return 1
    def d():
        return 1
    def e():
        return 1
    
class I(H):
    def f():
        return 1
    def g():
        return 1