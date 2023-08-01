package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.DishService;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/7/28 14:55
 */
@Service
@Slf4j
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        // 用户只能查询自己的购物车
        shoppingCart.setUserId(BaseContext.getCurrentId());

        // 判断当前加入的商品是否已经再购物车中
        List<ShoppingCart> list = list(shoppingCart);
        if(list != null && list.size() == 1){
            // 如果已经存在，则数量加一
            // 在查询用户购物车时，是根据 userId, dishId, dishFlavor, setmealId 动态查询的(后三个值都是前端传过来的)。查询的结果只可能时一条或者零条
            shoppingCart = list.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            // 更新购物车
            shoppingCartMapper.updateById(shoppingCart);
        }else{
            // 如果不存在，需要插入一条购物车数据
            // 判断当前添加的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                // 添加的是菜品
                Dish dish = dishService.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else{
                // 添加的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.selectById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }

            // 购物车插入一条数据
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 动态查询购物车数据
     * @param shoppingCart
     * @return
     */
    @Override
    public List<ShoppingCart> list(ShoppingCart shoppingCart) {
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        Long userId = shoppingCart.getUserId();
        Long dishId = shoppingCart.getDishId();
        String dishFlavor = shoppingCart.getDishFlavor();
        Long setmealId = shoppingCart.getSetmealId();
        lqw.eq(null != shoppingCart.getUserId(), ShoppingCart::getUserId, userId);
        lqw.eq(null != shoppingCart.getDishId(), ShoppingCart::getDishId, dishId);
        lqw.eq(StringUtils.isNotEmpty(dishFlavor), ShoppingCart::getDishFlavor, dishFlavor);
        lqw.eq(null != shoppingCart.getSetmealId(), ShoppingCart::getSetmealId, setmealId);
        lqw.orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartMapper.selectList(lqw);
        return list;
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        // 获取当前用户 id
        Long userId = BaseContext.getCurrentId();
        lqw.eq(null != userId, ShoppingCart::getUserId, userId);
        // 根据用户 id 查询对应的购物车信息
        List<ShoppingCart> cartList = shoppingCartMapper.selectList(lqw);
        return cartList;
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppingCart() {
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        // 删除当前用户的所有购物车数据，如果设有缓存，则缓存中的信息也要删除
        Long userId = BaseContext.getCurrentId();
        lqw.eq(null != userId, ShoppingCart::getUserId, userId);
        shoppingCartMapper.delete(lqw);

    }
}
