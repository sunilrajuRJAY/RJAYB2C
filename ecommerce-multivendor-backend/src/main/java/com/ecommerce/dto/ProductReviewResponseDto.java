package com.ecommerce.dto;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.entity.Review;

public class ProductReviewResponseDto extends CommonApiResponse {

	private List<Review> reviews = new ArrayList<>();

	private double averageRating = 0.0;

	public List<Review> getReviews() {
		return reviews;
	}

	public void setReviews(List<Review> reviews) {
		this.reviews = reviews;
	}

	public double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(double averageRating) {
		this.averageRating = averageRating;
	}

}
