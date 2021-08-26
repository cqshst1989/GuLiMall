package com.taoji666.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

//ConstraintValidator<A extends Annotation, T> ，泛型具体化以后，A必须是Annotation的子类，这里是ListValue注解，这个注解标记在Integer上面
public class ListValueConstraintValidator implements ConstraintValidator<ListValue,Integer> {

    private Set<Integer> set = new HashSet<>();
    //初始化方法
    @Override
    public void initialize(ListValue constraintAnnotation) {

        //通过注解将，我们指定的校验规则（这里是0,1），遍历出来，放进hashset数组set
        int[] vals = constraintAnnotation.vals();
        for (int val : vals) {
            set.add(val);
        }

    }

    //判断是否校验成功
    //将实际传来的值（前端传来的 value），拿出来和set里面的（0,1）进行比较（校验）

    /**
     *
     * @param value 需要校验的值
     * @param context
     * @return
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {

        return set.contains(value);
    }
}
