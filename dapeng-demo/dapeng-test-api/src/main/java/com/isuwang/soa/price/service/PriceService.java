
      package com.isuwang.soa.price.service;

      import com.github.dapeng.core.Processor;
      import com.github.dapeng.core.Service;
      import com.github.dapeng.core.SoaGlobalTransactional;

      /**
       * Autogenerated by Dapeng-Code-Generator (2.0.4)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

      * 
      **/
      @Service(name="com.isuwang.soa.price.service.PriceService",version = "1.0.0")
      @Processor(className = "com.isuwang.soa.price.PriceServiceCodec$Processor")
      public interface PriceService {
      
          /**
          * 
          **/
          
          
            void insertPrice( com.isuwang.soa.price.domain.Price price) throws com.github.dapeng.core.SoaException;
          
        
          /**
          * 
          **/
          
          
            java.util.List<com.isuwang.soa.price.domain.Price> getPrices() throws com.github.dapeng.core.SoaException;
          
        
    }
    