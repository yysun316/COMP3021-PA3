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
    def a():  # a1 
        return 1
    def b():  # b1
        return 1