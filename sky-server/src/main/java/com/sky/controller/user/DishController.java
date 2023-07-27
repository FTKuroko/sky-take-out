package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/25 19:20
 */
@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "用户端菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    /**
     * 根据分类 id 查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类 id 查询菜品")
    public Result<List<DishVO>> list(Long categoryId){
        Dish dish = new Dish();
        // 只查询在售卖的菜品
        dish.setStatus(StatusConstant.ENABLE);
        dish.setCategoryId(categoryId);
        List<DishVO> dishVOList = dishService.listCache(dish);
        return Result.success(dishVOList);
    }
}
