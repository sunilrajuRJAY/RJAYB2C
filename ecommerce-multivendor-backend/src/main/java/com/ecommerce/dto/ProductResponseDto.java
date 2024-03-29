package com.ecommerce.dto;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.entity.Product;

public class ProductResponseDto extends CommonApiResponse {

	private List<Product> products = new ArrayList<>();

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}

}
