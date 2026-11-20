import type { Product } from '../../src/domain/product.js';

const electronics = { slug: 'electronics', name: 'Electronics' };
const mens = { slug: 'mens-clothing', name: "Men's clothing" };
const jewellery = { slug: 'jewelery', name: 'Jewelery' };

function product(id: string, title: string, category: Product['category'], rating: number, description = ''): Product {
  return {
    id,
    title,
    description: description || `Description of ${title}`,
    price: { amount: 1999, currency: 'USD' },
    category,
    rating: { average: rating, count: 100 },
    imageSourceUrl: `https://images.example.test/${id}.jpg`,
  };
}

export const fixtureProducts: Product[] = [
  product('1', 'Mens Cotton Jacket', mens, 4.7, 'Great outerwear for spring or autumn'),
  product('2', 'Slim Fit T-Shirt', mens, 4.1),
  product('3', 'Casual Shirt', mens, 2.1),
  product('4', 'Silver Dragon Bracelet', jewellery, 4.6),
  product('5', 'Gold Ring', jewellery, 3.0),
  product('6', 'Portable SSD 1TB', electronics, 4.8, 'Fast external storage'),
  product('7', 'Gaming Drive 4TB', electronics, 4.0),
  product('8', 'Curved Monitor', electronics, 2.2),
];
