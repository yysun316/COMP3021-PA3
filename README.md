# COMP3021 Spring 2024 Java Programming Assignment 3 (PA3)

## Python AST Management System

**[AST (Abstract Syntax Tree)](https://en.wikipedia.org/wiki/Abstract_syntax_tree)** is a tree representation that represents the syntactic structure of source code. It is widely used in compilers and interpreters to reason about relationships between program elements. In this project, you are required to implement your own management system, named **ASTManager**, for parsing and analyzing Python ASTs.

### Grading System

In PA3, **ASTManager** should support the following functionalities:

- Task 1: ... (40%)
- Task 2: ... (5 * 10% = 50%)
- Task 3: ... (LoC) (10%)
- Bonus Task: ... (10%)

Each test case is an XML file that represents a Python AST. We provide public test cases for you to verify the correctness of your implementations. However, passing all the public test cases does not mean that you can obtain the full mark for the PA. We will also provide many additional test cases as the hidden ones, which are different from the ones we provided in the skeleton.

Before task specification, we first explain the grading policy as follows for your reference so that you will not miss it.

| Item                                                      | Ratio | Notes                                                        |
| --------------------------------------------------------- | ----- | ------------------------------------------------------------ |
| Keeping your GitHub repository private                    | 5%    | You must keep your repository **priavte** at all times.      |
| Having at least three commits on different days           | 5%    | You should commit three times during different days in your repository |
| Code style                                                | 10%   | You get 10% by default, and every 5 warnings from CheckStyle deducts 1%. |
| Public test cases (Task 1 + Task 2 + Task 3 + Bonus Task) | 30%   | (# of passing tests / # of provided tests) * 30%             |
| Hidden test cases (Task 1 + Task 2 + Task 3 + Bonus Task) | 50%   | (# of passing tests / # of provided tests) * 50%             |


### Submission Policy

Please submit your code on Canvas before the deadline **May 4, 2024, 23:59:59.** You should submit a single text file specified as follows:

- A file named `<itsc-id>.txt` containing the URL of your private repository at the first line. We will ask you to add the TAs' accounts as collaborators near the deadline.

For example, a student CHAN, Tai Man with ITSC ID `tmchanaa` having a repository at `https://github.com/tai-man-chan/COMP3021-PA3` should submit a file named `tmchanaa.txt` with the following content:

```txt
https://github.com/tai-man-chan/COMP3021-PA3
```

Note that we are using automatic scripts to process your submission on test cases rather than testing via the console manually. **DO NOT add extra explanation** to the file; otherwise, they will prevent our scripts from correctly processing your submission. 

**We will grade your submission based on the latest committed version before the deadline.** Please make sure all the amendments are made before the deadline and do not make changes after the deadline.

We have pre-configured a gradle task to check style for you. You can run `./gradlew checkstyleMain` in the integrated terminal of IntelliJ to check style.

Before submission, please make sure that: 

1. Your code can be complied with successfully. Please try to compile your code with `./gradlew build` before submission. You will not get any marks for public/hidden test cases if your code cannot be successfully compiled.

2. Your implementation can pass the public test cases we provided in `src/test`.

3. Your implementation should not yield too many errors when running `./gradlew checkstyleMain`.

### Academic Integrity

We trust that you are familiar with the Honor Code of HKUST. If not, refer to [this page](https://course.cse.ust.hk/comp3021/#policy).

### Contact US

If you have any questions on the PA1, please email TA Wei Chen via wei.chen@connect.ust.hk

---

Last Update: April 14, 2024
