class A:
    def a():
        return 1
    
class B(A):
    def a():  # a1
        return 1
    def b():
        return 1
        
class C(B):
    def a():  # a2
        return 1
    def c():
        return 1
    
class D(C):
    def b():  # b1
        return 1
    def c():  # c1
        return 1
    def d():
        return 1
    
class E(C):
    def a(): # a3
        return 1
    def b():  # b2
        return 1
    def e():
        return 1
    
class F:
    def f():
        return 1

class G(F):
    def g():
        return 1
    
    
class H(E, G):
    def c():  # c2
        return 1
    def e():  # e1
        return 1
    def f():  # f1
        return 1
    def h():
        return 1