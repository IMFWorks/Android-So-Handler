package com.imf.plugin.so;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor

import static org.objectweb.asm.Opcodes.*;

class LoadLibraryVisitor extends ClassVisitor {
    static final String TARGET_FLAG = "java/lang/System";
    static final String LOAD_LIBRARY = "loadLibrary";
    static final String LOAD = "load";
    static final String SO_LOAD_HOOK = "com/imf/so/SoLoadHook";
    static final String SO_LOAD_INTERFACES = "com/imf/so/SoLoadProxy";
    static final String ANNOTATION = "Lcom/imf/so/KeepSystemLoadLib;";
    boolean isClassSkip = false;
    private String className;

    public LoadLibraryVisitor(ClassVisitor cv) {
        super(ASM5, cv);
    }
    /**
     * 访问类头部信息
     * @param version JDK 版本 8-52
     * @param access 类修饰符 public
     * @param name 类名称 路径形式 如java/lang/System
     * @param signature 泛型信息
     * @param superName 所继承的类
     * @param interfaces 所实现的接口
     */
    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        if (interfaces) {
            for (int i = 0; i < interfaces.length; i++) {
                isClassSkip = SO_LOAD_INTERFACES.equals(interfaces[i])
                if (isClassSkip) {
                    break
                }
            }
        }
        className = name
    }

    /**
     * 访问类的注解
     *
     * @param desc
     *            注解类的类描述
     * @param visible
     *            runtime时期注解是否可以被访问
     * @return 返回一个注解值访问器
     */
    @Override
    AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (!visible && !isClassSkip && ANNOTATION.equals(descriptor)) {
            isClassSkip = true;
            println "类${className} isClassSkip = ${isClassSkip} ,注解信息 descriptor=${descriptor} , visible=$visible"
        }
        return super.visitAnnotation(descriptor, visible);
    }

    /**
     *
     * @param access
     * @param name
     * @param desc 方法签名 如Ljava/lang/String;
     * @param signature
     * @param exceptions 如果方法可能会抛出的异常
     * @return
     */
    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        //这里我们修改了MethodVisitor再返回，即修改了这个方法
        return isClassSkip ? mv : new MethodVisitor(ASM5, mv) {
            private isMethodSkip = false;

            @Override
            AnnotationVisitor visitAnnotation(String annotationDesc, boolean visible) {
                if (!visible) {
                    isMethodSkip = ANNOTATION.equals(annotationDesc)
                }
                if(isMethodSkip){
                    println "方法${className}.${name} 注解 isMethodSkip = ${isMethodSkip} , descriptor=${annotationDesc} , visible=$visible"
                }
                return super.visitAnnotation(annotationDesc, visible)
            }

            @Override
            void visitMethodInsn(int opcode, String owner, String methodName, String methodDesc, boolean itf) {
                //覆盖方法调用的过程
                if (!isMethodSkip && opcode == INVOKESTATIC //
                        && LoadLibraryVisitor.TARGET_FLAG.equals(owner) //
                        && (LoadLibraryVisitor.LOAD_LIBRARY.equals(methodName) || LoadLibraryVisitor.LOAD.equals(methodName))) {
                    owner = LoadLibraryVisitor.SO_LOAD_HOOK
                    println "[So Load Hook到目标类]: ${className}.${methodName}"
                }
                super.visitMethodInsn(opcode, owner, methodName, methodDesc, itf);
            }
        }
    }
}