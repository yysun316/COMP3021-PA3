class A:
    def a():
        return 1
    
class B(A):
    def b():
        return 1
    
class C:
    def c():
        return 1
    
class D(A, C):
    def b():  
        return 1
    
class E(B, C):
    def e():
        return 1

class F(C):
    def f():   
        return 1
    
class G(A):
    def b():  
        return 1

class H(F, G):
    def c():
        return 1
