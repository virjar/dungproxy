package com.mantou.proxyservice.proxeservice.repository;

import com.virjar.dungproxy.server.entity.DomainIp;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.repository.DomainIpRepository;
import com.virjar.dungproxy.server.repository.ProxyRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * Description: DomainIpRepositoryTest
 *
 * @author lingtong.fu
 * @version 2016-11-11 17:52
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext.xml"})
public class DomainIpRepositoryTest {

    @Resource
    private DomainIpRepository domainIpRepository;

    @Resource
    private ProxyRepository proxyRepository;

    @Test
    public void test() {
        System.out.println("Hello World!");
        PageRequest pageRequest = new PageRequest(0, Integer.MAX_VALUE);
        List<DomainIp> domainIpList = domainIpRepository.selectAvailable("www.66ip.cn", pageRequest);
        Long proxyId = domainIpList.get(0).getProxyId();
        //return proxyRepository.selectByPrimaryKey(proxyId);
        /*for (DomainIp domainIp: domainIpList) {
            System.out.println("domainIp is :" + domainIp.getIp());
        }*/
        System.out.println("Proxy id is : " + String.valueOf(proxyId));
        Proxy proxy = proxyRepository.selectByPrimaryKey(proxyId);
        System.out.println(proxy.toString());
    }
}
