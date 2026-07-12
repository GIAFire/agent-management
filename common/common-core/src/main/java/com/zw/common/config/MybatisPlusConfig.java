package com.zw.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    // 分页插件
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                new TenantLineHandler() {
                    @Override
                    public Expression getTenantId() {
                        // 从当前上下文中获取租户ID
                        UserInfo userInfo = UserContext.get();
                        return new LongValue(userInfo != null ? userInfo.getTenantId() : 0L);
                    }

                    @Override
                    public String getTenantIdColumn() {
                        // 租户字段名
                        return "tenant_id";
                    }

                    @Override
                    public boolean ignoreTable(String tableName) {
                        // 哪些表不需要加租户条件
                        return "sys_config".equalsIgnoreCase(tableName)
                                || "sys_dict".equalsIgnoreCase(tableName)
                                || "sys_tenant".equalsIgnoreCase(tableName);
                    }
                }
        ));
        return interceptor;
    }

}
