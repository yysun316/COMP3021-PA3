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
    def a():  # a1
        return 1
    def b():  
        return 1
    def c(): # c1
        return 1
    
class E(B, C):
    def a():  # a2
        return 1
    def b():  # b1
        return 1
    def c():  # c2
        return 1

class F(C):
    def a():   
        return 1
    def b():  
        return 1
    def c():   # c3
        return 1
    
class G(A):
    def a():  #a3
        return 1
    def b():  
        return 1
    def c():
        return 1