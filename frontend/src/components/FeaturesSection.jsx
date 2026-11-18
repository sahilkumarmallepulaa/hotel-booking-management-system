import React from 'react';

const FeaturesSection = () => {
  const features = [
    {
      icon: '🛡️',
      title: 'Best Rate Guarantee',
      desc: 'Get the lowest negotiated prices with direct hotel partnerships and zero hidden booking fees.',
    },
    {
      icon: '✨',
      title: 'Verified Luxury Stays',
      desc: 'Every hotel and suite is rigorously evaluated to meet exacting standards of comfort and hygiene.',
    },
    {
      icon: '⚡',
      title: 'Instant Confirmation',
      desc: 'Real-time room availability powered by microservices ensuring zero double-bookings.',
    },
    {
      icon: '🛎️',
      title: '24/7 Dedicated Support',
      desc: 'Our hospitality concierge specialists are available around the clock for any assistance.',
    },
  ];

  return (
    <section className="features-section">
      <div className="features-grid">
        {features.map((feature, idx) => (
          <div key={idx} className="feature-item">
            <div className="feature-icon-box">{feature.icon}</div>
            <div>
              <h4 className="feature-title">{feature.title}</h4>
              <p className="feature-desc">{feature.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};

export default FeaturesSection;
