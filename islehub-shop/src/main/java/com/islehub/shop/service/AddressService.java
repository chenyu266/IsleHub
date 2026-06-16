package com.islehub.shop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.exception.BizException;
import com.islehub.shop.entity.Address;
import com.islehub.shop.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AddressService extends ServiceImpl<AddressMapper, Address> {

    public List<Address> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt));
    }

    @Transactional
    public void addAddress(Address address, Long userId) {
        address.setUserId(userId);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        save(address);
    }

    @Transactional
    public void updateAddress(Address address, Long userId) {
        Address exist = getById(address.getId());
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        address.setUserId(userId);
        updateById(address);
    }

    public void deleteAddress(Long id, Long userId) {
        Address exist = getById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        removeById(id);
    }

    private void clearDefault(Long userId) {
        update(new LambdaUpdateWrapper<Address>()
                .set(Address::getIsDefault, 0)
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1));
    }
}
