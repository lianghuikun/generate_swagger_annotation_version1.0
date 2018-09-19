import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ThrowableRunnable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 根据注释生成swagger注解
 */
public class SwaggerAnnotationAction extends AnAction {

    public final static String PREFIX = "@";
    public final static String API_MODEL_PROPERTY = "ApiModelProperty";
    public static final String MODEL = "ApiModel";

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取当前编辑的文件
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        // 获取项目
        Project project = e.getProject();
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
        PsiClass[] classes = Objects.requireNonNull(psiJavaFile).getClasses();
        Arrays.stream(classes)
                .forEach(cla -> {
                    try {
                        WriteCommandAction.writeCommandAction(project, psiFile)
                                .run((ThrowableRunnable<Throwable>) () -> {
                                    // 为类添加
                                    PsiDocComment classDoc = cla.getDocComment();
                                    PsiAnnotation psiAnnotation = cla.getAnnotation(SwaggerAnnotationAction.MODEL);
                                    if (psiAnnotation == null) {
                                        PsiElement[] descriptionElements = classDoc.getDescriptionElements();
                                        /*
                                            descriptionElements 为：
                                                        /*
                                                        * 游戏
                                                         / 而我们只想得到： 游戏二字
                                         */
                                        List<String> classTextList = Arrays.stream(descriptionElements)
                                                // 去除型号和换行
                                                .filter(text -> !(text instanceof PsiWhiteSpace))
                                                .map(PsiElement::getText)
                                                .collect(Collectors.toList());
                                        if (CollectionUtils.isNotEmpty(classTextList)) {
                                            String classText = classTextList.get(0);
                                            // 如果还没有为类添加注解
                                            // @Api(tags = {"督导下发指派表API"})
                                            String classStr = SwaggerAnnotationAction.PREFIX + SwaggerAnnotationAction.MODEL + "(tags = {\"" + classText + "\"})";
                                            PsiElementFactory classFactory = JavaPsiFacade.getElementFactory(project);
                                            PsiAnnotation classAnnotation = classFactory.createAnnotationFromText(classStr, cla);
                                            classDoc.addAfter(classAnnotation, classDoc);
                                        }
                                    }

                                });
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }


                    // 为属性添加注解
                    PsiField[] allFields = cla.getAllFields();
                    Arrays.stream(allFields)
                            .forEach(field -> {
                                // 获取属性名
                                String name = field.getName();
                                PsiDocComment docComment = field.getDocComment();
                                Arrays.stream(Objects.requireNonNull(docComment).getDescriptionElements())
                                        .map(PsiElement::getText)// 提取注释信息
                                        .filter(StringUtils::isNotBlank)
                                        .forEach(desc -> {
                                            // System.out.println("--------->1:" + field.getAnnotation("ApiModelProperty"));
                                            PsiAnnotation psiAnnotation = field.getAnnotation(SwaggerAnnotationAction.API_MODEL_PROPERTY);
                                            if (psiAnnotation == null) {
                                                // 生成直接字符串，eg：@ApiModelProperty(value=" 编码",name="id")
                                                String str = SwaggerAnnotationAction.PREFIX + SwaggerAnnotationAction.API_MODEL_PROPERTY + "(value=\"" + desc + "\",name=\"" + name + "\")";
                                                System.out.println("------------>:" + str);
                                                // 如果为空不存在@ApiModelProperty注解
                                                try {
                                                    WriteCommandAction.writeCommandAction(project, psiFile)
                                                            .run((ThrowableRunnable<Throwable>) () -> {
                                                                // 将注解写入文件中
                                                                PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                                                                PsiAnnotation annotationFromText = factory.createAnnotationFromText(str, cla);
                                                                // 获取修饰符, eg: private int age; 的private
                                                                PsiModifierList modifierList = field.getModifierList();
//                                                            modifierList.add(annotationFromText);
//                                                            docComment.addBefore(annotationFromText, field);
//                                                            docComment.addAfter(annotationFromText, field);
                                                                // A.addAfter(B, A) ,  意思是， 在A 后面 添加B。
                                                                docComment.addAfter(annotationFromText, docComment);
                                                            });
                                                } catch (Throwable throwable) {
                                                    throwable.printStackTrace();
                                                }
                                            } else {
                                                // 已经存在@ApiModelProperty注解了，不需要写入
                                            }
                                        });
                            });

                });
    }
}
