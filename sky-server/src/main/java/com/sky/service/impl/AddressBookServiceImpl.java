package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/1 15:34
 */
@Service
@Slf4j
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    /**
     * 动态查询地址列表
     * @param addressBook
     * @return
     */
    @Override
    public List<AddressBook> listAddress(AddressBook addressBook) {
        LambdaQueryWrapper<AddressBook> lqw = new LambdaQueryWrapper<>();
        // 动态查询用户地址信息，这里的业务需求可以根据用户 id、手机号以及是否是默认地址三个条件查询。
        Long userId = addressBook.getUserId();
        String phone = addressBook.getPhone();
        Integer isDefault = addressBook.getIsDefault();
        lqw.eq(null != userId, AddressBook::getUserId, userId);
        lqw.eq(StringUtils.isNotEmpty(phone), AddressBook::getPhone, phone);
        lqw.eq(null != isDefault, AddressBook::getIsDefault, isDefault);

        List<AddressBook> addressBookList = addressBookMapper.selectList(lqw);
        return addressBookList;
    }

    /**
     * 新增地址
     * @param addressBook
     */
    @Override
    public void saveAddress(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();
        // 设置地址所属用户
        addressBook.setUserId(userId);
        // 新增地址时默认为非默认地址
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据 id 查询地址
     * @param id
     * @return
     */
    @Override
    public AddressBook getAddressById(Long id) {
        AddressBook address = addressBookMapper.selectById(id);
        return address;
    }

    /**
     * 更新地址
     * @param addressBook
     */
    @Override
    public void updateAddress(AddressBook addressBook) {
        // 获取旧的地址信息
        Long id = addressBook.getId();
        AddressBook address = getAddressById(id);
        // 更新地址信息
        String consignee = addressBook.getConsignee();
        String sex = addressBook.getSex();
        String phone = addressBook.getPhone();
        String detail = addressBook.getDetail();
        String label = addressBook.getLabel();
        Integer isDefault = addressBook.getIsDefault();
        if(StringUtils.isNotEmpty(consignee)) address.setConsignee(consignee);
        if(StringUtils.isNotEmpty(sex)) address.setSex(sex);
        if(StringUtils.isNotEmpty(phone)) address.setPhone(phone);
        if(StringUtils.isNotEmpty(detail)) address.setDetail(detail);
        if(StringUtils.isNotEmpty(label)) address.setLabel(label);
        if(null != isDefault){
            address.setIsDefault(isDefault);
            // 如果此次更新将当前地址修改成默认地址，则原始的默认地址需要修改成非默认地址
            if(1 == isDefault){
                // 修改原始的默认地址,动态查询出原始默认地址
                AddressBook addressBook1 = getDefaultAddress();
                if(addressBook1 != null){
                    // 默认地址只有一个
                    addressBook1.setIsDefault(0);
                    addressBookMapper.updateById(addressBook1);
                }
            }
        }
        // 更新当前地址信息
        addressBookMapper.updateById(address);
    }

    /**
     * 设置默认地址
     * @param addressBook
     */
    @Override
    public void setDefaultAddress(AddressBook addressBook) {
        // 如果已经有默认地址了，则先修改原始的默认地址
        AddressBook address = getDefaultAddress();
        if(address != null){
            address.setIsDefault(0);
            addressBookMapper.updateById(address);
        }
        // 将当前地址设为默认地址
        addressBook.setIsDefault(1);
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 根据 id 删除地址
     * @param id
     */
    @Override
    public void deleteAddressById(Long id) {
        addressBookMapper.deleteById(id);
    }

    /**
     * 获取默认地址
     * @return
     */
    @Override
    public AddressBook getDefaultAddress() {
        LambdaQueryWrapper<AddressBook> lqw = new LambdaQueryWrapper<>();
        Long userId = BaseContext.getCurrentId();
        lqw.eq(null != userId, AddressBook::getUserId, userId);
        lqw.eq(AddressBook::getIsDefault, 1);
        AddressBook addressBook = addressBookMapper.selectOne(lqw);
        return addressBook;
    }
}
