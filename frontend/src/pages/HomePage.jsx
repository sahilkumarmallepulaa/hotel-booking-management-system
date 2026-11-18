import React from 'react';
import HeroSection from '../components/HeroSection';
import FeaturedHotels from '../components/FeaturedHotels';
import FeaturesSection from '../components/FeaturesSection';

const HomePage = () => {
  const handleSearch = (searchParams) => {
    console.log('Search initiated:', searchParams);
    // Smooth scroll to featured hotels
    const element = document.getElementById('featured-hotels');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div>
      <HeroSection onSearch={handleSearch} />
      <FeaturedHotels />
      <FeaturesSection />
    </div>
  );
};

export default HomePage;
