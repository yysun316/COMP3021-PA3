class A:
    def a():
        return 1
    
class B(A):
    def b():
        return 1
    
class C(B):
    def c():
        return 1
    
class D(C):
    def d():
        return 1
    
class E(C):
    def e():
        return 1
    
class F:
    def f():
        return 1

class G(F):
    def g():
        return 1
    
class H(E, G):
    def h():
        return 1
    
class I:
    def i():
        return 1

class J:
    def j():
        return 1
    
class K(I, J):
    def k():
        return 1

class L(K, F, B):
    def l():
        return 1

class M(J, I, D):
    def m():
        return 1
    
class N(K, H):
    def n():
        return 1

class O(C, G, I):
    def o():
        return 1