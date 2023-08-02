package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/1 15:32
 */
@RestController
@RequestMapping("/user/addressBook")
@Slf4j
@Api("地址管理功能接口")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     * @param addressBook
     * @return
     */
    @PostMapping
    @ApiOperation("新增地址")
    public Result<String> saveAddress(@RequestBody AddressBook addressBook){
        log.info("新增地址:{}", addressBook);
        addressBookService.saveAddress(addressBook);
        return Result.success();
    }

    /**
     * 查询用户所有地址
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("动态查询用户的所有地址")
    public Result<List<AddressBook>> addressList(){
        log.info("动态查询用户的所有地址:{}");
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> addressBookList = addressBookService.listAddress(addressBook);
        return Result.success(addressBookList);
    }

    /**
     * 查询默认地址
     * @return
     */
    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> getDefaultAddress(){
        log.info("查询默认地址");
        AddressBook defaultAddress = addressBookService.getDefaultAddress();
        return Result.success(defaultAddress);
    }

    /**
     * 修改地址
     * @param addressBook
     * @return
     */
    @PutMapping
    @ApiOperation("修改地址")
    public Result<String> updateAddress(@RequestBody AddressBook addressBook){
        log.info("修改地址:{}", addressBook);
        addressBookService.updateAddress(addressBook);
        return Result.success();
    }

    /**
     * 根据 id 删除地址
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据 id 删除地址")
    public Result<String> deleteAddressById(Long id){
        log.info("根据 id 删除地址:{}", id);
        addressBookService.deleteAddressById(id);
        return Result.success();
    }

    /**
     * 根据 id 查询地址
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据 id 查询地址")
    public Result<AddressBook> getAddressById(@PathVariable Long id){
        log.info("根据 id 查询地址:{}", id);
        AddressBook address = addressBookService.getAddressById(id);
        return Result.success(address);
    }

    /**
     * 设置默认地址
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result<String> setDefaultAddress(@RequestBody AddressBook addressBook){
        log.info("设置默认地址:{}", addressBook);
        addressBookService.setDefaultAddress(addressBook);
        return Result.success();
    }
}
