import React from 'react';
import { useParams } from 'react-router';

export const BookRoute: React.FC = () => {
  const { bookId } = useParams<{ bookId: string }>();

  return (
    <div>
      {bookId}
    </div>
  );
};
